package com.cipherforge.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cipherforge.crypto.AesCipher
import com.cipherforge.crypto.EcdhKeyExchange
import com.cipherforge.crypto.KeyCodec
import java.security.KeyPair
import javax.crypto.spec.SecretKeySpec

/**
 * Практический сценарий обмена ключами (ECDH): генерируешь свой ключ, передаёшь
 * свой публичный ключ собеседнику любым каналом (сообщение, почта, QR — в будущем),
 * вставляешь присланный им публичный ключ — и обе стороны независимо получают
 * один и тот же общий AES-ключ.
 */
class EcdhViewModel : ViewModel() {

    var myKeyPair by mutableStateOf<KeyPair?>(null)
        private set
    var theirPublicKeyInput by mutableStateOf("")
    var sharedKey by mutableStateOf<SecretKeySpec?>(null)
        private set
    var inputText by mutableStateOf("")
    var outputText by mutableStateOf("")
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    val myPublicKeyEncoded: String?
        get() = myKeyPair?.let { KeyCodec.encodePublicKey(it.public) }

    fun generateMyKeyPair() {
        myKeyPair = EcdhKeyExchange.generateKeyPair()
        sharedKey = null
        errorText = null
    }

    fun deriveSharedKey() {
        errorText = null
        val myKp = myKeyPair
        if (myKp == null) {
            errorText = "Сначала сгенерируй свой ключ"
            return
        }
        sharedKey = try {
            val theirPublicKey = KeyCodec.decodePublicKey(theirPublicKeyInput.trim(), "EC")
            EcdhKeyExchange.deriveSharedAesKey(myKp.private, theirPublicKey)
        } catch (e: Exception) {
            errorText = "Некорректный публичный ключ собеседника"
            null
        }
    }

    fun encrypt() = runOperation {
        val key = sharedKey ?: error("Сначала согласуй общий ключ с собеседником")
        AesCipher.encrypt(inputText, key)
    }

    fun decrypt() = runOperation {
        val key = sharedKey ?: error("Сначала согласуй общий ключ с собеседником")
        AesCipher.decrypt(inputText, key)
    }

    private inline fun runOperation(block: () -> String) {
        errorText = null
        outputText = try {
            block()
        } catch (e: Exception) {
            errorText = "Ошибка: ${e.message}"
            ""
        }
    }
}
