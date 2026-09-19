package com.example.ui.common

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import java.io.File

/**
 * Componente unificado de Avatar de Usuario en toda la aplicación:
 * - La imagen rellena al 100% el espacio circular sin rellenos blancos ni marcos vacíos (ContentScale.Crop).
 * - Admite fotos locales, URLs remotas de Supabase, cadenas base64, presets de avatares (avatarchico / avatarchica) y fallbacks.
 * - El contorno se aplica sin desfasar ni recortar la imagen interior.
 */
@Composable
fun UserAvatarImage(
    avatarUrl: String?,
    displayName: String = "Jugador",
    fallbackDrawable: Int = R.drawable.avatarchico,
    size: Dp = 40.dp,
    borderBrush: Brush? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    // 1. Detectar si es un preset especial guardado
    val presetDrawable = when (avatarUrl) {
        "preset:avatarchico" -> R.drawable.avatarchico
        "preset:avatarchica" -> R.drawable.avatarchica
        else -> null
    }

    // 2. Decodificar imagen si es base64
    val base64Bitmap = remember(avatarUrl) {
        if (avatarUrl != null && avatarUrl.startsWith("data:image/") && avatarUrl.contains("base64,")) {
            try {
                val base64Data = avatarUrl.substringAfter("base64,")
                val decoded = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    // 3. Decodificar archivo local si es una ruta absoluta
    val localFileBitmap = remember(avatarUrl) {
        if (presetDrawable == null && base64Bitmap == null &&
            !avatarUrl.isNullOrBlank() && !avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")
        ) {
            try {
                val file = File(avatarUrl)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    var rootModifier = modifier
        .size(size)
        .clip(CircleShape)

    if (borderBrush != null) {
        rootModifier = rootModifier.border(borderWidth, borderBrush, CircleShape)
    } else if (borderColor != null) {
        rootModifier = rootModifier.border(borderWidth, borderColor, CircleShape)
    }

    Box(
        modifier = rootModifier.background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Preset manual seleccionado (chico o chica)
            presetDrawable != null -> {
                Image(
                    painter = painterResource(id = presetDrawable),
                    contentDescription = "Avatar de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Imagen codificada en Base64
            base64Bitmap != null -> {
                Image(
                    bitmap = base64Bitmap,
                    contentDescription = "Avatar de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Imagen desde archivo local cargado por el usuario
            localFileBitmap != null -> {
                Image(
                    bitmap = localFileBitmap,
                    contentDescription = "Avatar de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Imagen remota desde Supabase Storage o URL web
            !avatarUrl.isNullOrBlank() && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) -> {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Fallback por defecto (avatarchico o avatarchica)
            else -> {
                Image(
                    painter = painterResource(id = fallbackDrawable),
                    contentDescription = "Avatar de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

