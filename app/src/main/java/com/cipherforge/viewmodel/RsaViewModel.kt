package com.cipherforge.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cipherforge.crypto.KeyCodec
import com.cipherforge.crypto.RsaCipher
import java.security.KeyPair

/**
 * Практический сценарий: "мой ключ" генерируется один раз и используется для приёма
 * сообщений (им шифруют тебе), "ключ получателя" вставляется вручную или сканируется
 * по QR — им шифруется то, что отправляешь ты. Расшифровка всегда идёт своим приватным ключом.
 */
class RsaViewModel : ViewModel() {

    var keyPair by mutableStateOf<KeyPair?>(null)
        private set
    var recipientPublicKeyInput by mutableStateOf("")
    var inputText by mutableStateOf("")
    var outputText by mutableStateOf("")
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    val myPublicKeyEncoded: String?
        get() = keyPair?.let { KeyCodec.encodePublicKey(it.public) }

    fun generateKeys() {
        keyPair = RsaCipher.generateKeyPair()
    }

    fun encrypt() = runOperation {
        val publicKey = KeyCodec.decodePublicKey(recipientPublicKeyInput.trim(), "RSA")
        RsaCipher.encryptHybrid(inputText, publicKey)
    }

    fun decrypt() = runOperation {
        val kp = keyPair ?: error("Сначала сгенерируй свой ключ")
        RsaCipher.decryptHybrid(inputText, kp.private)
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
