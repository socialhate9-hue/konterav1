package com.example.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.media.ExifInterface
import com.example.stats.PlayerStatsManager
import com.example.supabase.SupabaseAuthManager
import com.example.supabase.SupabaseClient
import com.example.supabase.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object AvatarManager {
    private const val TAG = "AvatarManager"

    /**
     * Procesa una imagen seleccionada desde la galería:
     * - Lee la URI, corrige la rotación EXIF
     * - Centra y recorta en formato cuadrado y reescala a 512x512
     * - Guarda la imagen en almacenamiento local persistente
     * - Genera miniatura en base64
     * - Actualiza de inmediato el estado local
     * - Sube y sincroniza con Supabase Storage y la tabla 'profiles'
     */
    suspend fun saveAndUploadAvatar(
        context: Context,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            // 1. Orientación EXIF
            var rotationDegrees = 0
            try {
                resolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo leer EXIF", e)
            }

            // 2. Decodificar Bitmap
            val originalBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext Result.failure(Exception("No se pudo decodificar la imagen seleccionada"))

            // 3. Rotar según EXIF
            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            } else {
                originalBitmap
            }

            // 4. Recortar cuadrado centrado
            val minDim = minOf(rotatedBitmap.width, rotatedBitmap.height)
            val xOffset = (rotatedBitmap.width - minDim) / 2
            val yOffset = (rotatedBitmap.height - minDim) / 2
            val squareBitmap = Bitmap.createBitmap(rotatedBitmap, xOffset, yOffset, minDim, minDim)

            // 5. Escalar a 512x512 para excelente nitidez y rendimiento óptimo
            val scaledBitmap = Bitmap.createScaledBitmap(squareBitmap, 512, 512, true)

            // 6. Obtener ID de usuario para guardar el archivo con nombre único
            val currentUser = SupabaseAuthManager.currentUser.value
            val userId = currentUser?.id?.ifBlank { null }
                ?: PlayerStatsManager.stats.value.userId.ifBlank { null }
                ?: "local_player"

            val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
            val localFile = File(avatarsDir, "avatar_${userId}.jpg")
            FileOutputStream(localFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            val localPath = localFile.absolutePath

            // 7. Actualizar localmente de inmediato para feedback instantáneo
            PlayerStatsManager.updateAvatarUrl(localPath)
            SupabaseAuthManager.updateCurrentUserAvatarUrl(localPath)

            // 8. Generar miniatura base64 por si Supabase Storage no tiene el bucket configurado
            val thumbBitmap = Bitmap.createScaledBitmap(scaledBitmap, 128, 128, true)
            val thumbStream = ByteArrayOutputStream()
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 75, thumbStream)
            val base64DataUri = "data:image/jpeg;base64," + Base64.encodeToString(thumbStream.toByteArray(), Base64.NO_WRAP)

            // 9. Comprimir imagen para subida a Supabase
            val uploadStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, uploadStream)
            val imageBytes = uploadStream.toByteArray()

            // 10. Intentar subir a Supabase Storage
            var finalSupabaseUrl = base64DataUri
            val storageResult = SupabaseClient.uploadAvatar(
                userId = userId,
                imageBytes = imageBytes,
                fileName = "avatar_${userId}.jpg"
            )

            if (storageResult.isSuccess) {
                finalSupabaseUrl = storageResult.getOrThrow()
                // Actualizar a la URL pública si la subida fue exitosa
                PlayerStatsManager.updateAvatarUrl(finalSupabaseUrl)
                SupabaseAuthManager.updateCurrentUserAvatarUrl(finalSupabaseUrl)
            } else {
                Log.w(TAG, "Storage no disponible, usando almacenamiento en perfil base64")
            }

            // 11. Guardar avatarUrl en la tabla 'profiles' de Supabase
            val playerName = currentUser?.username?.ifBlank { null }
                ?: PlayerStatsManager.stats.value.playerName
            val email = currentUser?.email.orEmpty()

            val profileUpdateResult = SupabaseClient.createOrUpdateProfile(
                UserProfile(
                    id = userId,
                    email = email,
                    username = playerName,
                    avatarUrl = finalSupabaseUrl
                )
            )

            if (profileUpdateResult.isSuccess) {
                Log.d(TAG, "Perfil de Supabase sincronizado con éxito con nuevo avatar")
            } else {
                Log.w(TAG, "No se pudo actualizar la tabla de profiles de Supabase", profileUpdateResult.exceptionOrNull())
            }

            Result.success(localPath)
        } catch (e: Exception) {
            Log.e(TAG, "Error en saveAndUploadAvatar", e)
            Result.failure(e)
        }
    }

    /**
     * Establece un preset predeterminado (chico o chica) o elimina la foto personalizada
     */
    suspend fun setAvatarPreset(
        context: Context,
        preset: String? // "preset:avatarchico", "preset:avatarchica", o null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = SupabaseAuthManager.currentUser.value
            val userId = currentUser?.id?.ifBlank { null }
                ?: PlayerStatsManager.stats.value.userId.ifBlank { null }
                ?: "local_player"

            // Si es null o preset, eliminamos el archivo local personalizado si existe
            if (preset == null || preset.startsWith("preset:")) {
                val file = File(context.filesDir, "avatars/avatar_${userId}.jpg")
                if (file.exists()) file.delete()
            }

            // Actualizar localmente
            PlayerStatsManager.updateAvatarUrl(preset)
            SupabaseAuthManager.updateCurrentUserAvatarUrl(preset)

            // Sincronizar en Supabase profiles
            val playerName = currentUser?.username?.ifBlank { null }
                ?: PlayerStatsManager.stats.value.playerName
            val email = currentUser?.email.orEmpty()

            SupabaseClient.createOrUpdateProfile(
                UserProfile(
                    id = userId,
                    email = email,
                    username = playerName,
                    avatarUrl = preset.orEmpty()
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error en setAvatarPreset", e)
            Result.failure(e)
        }
    }
}
