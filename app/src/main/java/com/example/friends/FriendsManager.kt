package com.example.friends

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.home.BattleRival
import com.example.home.RivalCategory
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestor centralizado de amigos y códigos de amigo de Kantera Basketball.
 * Permite a los jugadores:
 * - Tener su propio código de amigo único (ej. KANTERA-782) para compartir por WhatsApp o en persona.
 * - Agregar amigos introduciendo el código del amigo.
 * - Sincronizar automáticamente la lista con la pantalla de Batallas y el Perfil.
 */
object FriendsManager {

    private const val PREFS_NAME = "kantera_friends_prefs"
    private const val KEY_MY_FRIEND_CODE = "my_friend_code"
    private const val KEY_FRIENDS_JSON = "saved_friends_json"

    // Lista por defecto de compañeros de club / amigos iniciales
    private val DEFAULT_FRIENDS = listOf(
        BattleRival("f1", "Marc_99", "Nivel 4 · 540 Hype", RivalCategory.FRIENDS, "preset:avatarchico", true),
        BattleRival("f2", "Carlos Dribble", "Nivel 3 · 420 Hype", RivalCategory.FRIENDS, "preset:avatarchica", true),
        BattleRival("f3", "Hugo Splash", "Nivel 2 · 380 Hype", RivalCategory.FRIENDS, "preset:avatarchico", true),
        BattleRival("f4", "Lucas Basket", "Nivel 2 · 310 Hype", RivalCategory.FRIENDS, "preset:avatarchico", false)
    )

    private val _friendsState = mutableStateListOf<BattleRival>()
    val friendsList: SnapshotStateList<BattleRival> get() = _friendsState

    private var isInitialized = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Inicializa la lista de amigos desde almacenamiento local si no está cargada.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_FRIENDS_JSON, null)

        _friendsState.clear()
        if (jsonStr.isNullOrBlank()) {
            _friendsState.addAll(DEFAULT_FRIENDS)
            saveFriends(context)
        } else {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    _friendsState.add(
                        BattleRival(
                            id = obj.optString("id", "friend_$i"),
                            name = obj.optString("name", "Amigo"),
                            detail = obj.optString("detail", "Nivel 1"),
                            category = RivalCategory.FRIENDS,
                            avatarUrl = obj.optString("avatarUrl", "preset:avatarchico"),
                            isOnline = obj.optBoolean("isOnline", true)
                        )
                    )
                }
                if (_friendsState.isEmpty()) {
                    _friendsState.addAll(DEFAULT_FRIENDS)
                }
            } catch (e: Exception) {
                _friendsState.clear()
                _friendsState.addAll(DEFAULT_FRIENDS)
            }
        }
        isInitialized = true
    }

    private fun saveFriends(context: Context) {
        val prefs = getPrefs(context)
        val array = JSONArray()
        _friendsState.forEach { friend ->
            val obj = JSONObject().apply {
                put("id", friend.id)
                put("name", friend.name)
                put("detail", friend.detail)
                put("avatarUrl", friend.avatarUrl)
                put("isOnline", friend.isOnline)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_FRIENDS_JSON, array.toString()).apply()
    }

    /**
     * Devuelve o crea el código de amigo único del usuario.
     */
    fun getMyFriendCode(context: Context): String {
        val prefs = getPrefs(context)
        var code = prefs.getString(KEY_MY_FRIEND_CODE, null)
        if (code.isNullOrBlank()) {
            val randomNum = (100..999).random()
            code = "KANTERA-$randomNum"
            prefs.edit().putString(KEY_MY_FRIEND_CODE, code).apply()
        }
        return code
    }

    /**
     * Genera un nuevo código para el usuario.
     */
    fun regenerateMyFriendCode(context: Context): String {
        val prefs = getPrefs(context)
        val randomNum = (100..999).random()
        val newCode = "KANTERA-$randomNum"
        prefs.edit().putString(KEY_MY_FRIEND_CODE, newCode).apply()
        return newCode
    }

    /**
     * Añade un nuevo amigo utilizando su código.
     * Soporta formatos como KANTERA-123, BASKET-456, o cualquier texto identificativo.
     */
    fun addFriendByCode(context: Context, rawCode: String): Pair<Boolean, String> {
        initialize(context)
        val code = rawCode.trim().uppercase()
        if (code.length < 3) {
            return Pair(false, "El código debe tener al menos 3 caracteres.")
        }

        val myCode = getMyFriendCode(context)
        if (code == myCode) {
            return Pair(false, "¡Ese es tu propio código! Pásaselo a un amigo.")
        }

        // Crear nombre del amigo basado en el código
        val cleanSuffix = if (code.contains("-")) code.substringAfter("-") else code.takeLast(4)
        val friendName = if (code.contains("-")) {
            val prefix = code.substringBefore("-").lowercase().replaceFirstChar { it.uppercase() }
            "$prefix $cleanSuffix"
        } else {
            "Jugador $code"
        }

        // Comprobar si ya existe
        val exists = _friendsState.any { it.name.equals(friendName, ignoreCase = true) || it.id == "code_$code" }
        if (exists) {
            return Pair(false, "¡Ya tienes a este jugador en tu lista de amigos!")
        }

        val randomAvatar = if ((0..1).random() == 0) "preset:avatarchico" else "preset:avatarchica"
        val randomLevel = (1..5).random()
        val randomHype = (200..750).random()

        val newFriend = BattleRival(
            id = "code_$code",
            name = friendName,
            detail = "Nivel $randomLevel · $randomHype Hype · Añadido por código",
            category = RivalCategory.FRIENDS,
            avatarUrl = randomAvatar,
            isOnline = true
        )

        _friendsState.add(0, newFriend)
        saveFriends(context)
        return Pair(true, "¡$friendName añadido a tus amigos con éxito!")
    }

    /**
     * Elimina a un amigo de la lista.
     */
    fun removeFriend(context: Context, friendId: String) {
        initialize(context)
        _friendsState.removeAll { it.id == friendId }
        saveFriends(context)
    }
}
