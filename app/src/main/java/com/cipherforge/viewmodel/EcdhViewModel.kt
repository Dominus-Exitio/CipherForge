package com.cipherforge.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cipherforge.crypto.AesCipher
import com.cipherforge.crypto.EcdhKeyExchange
import java.security.KeyPair
import javax.crypto.SecretKey

class EcdhViewModel : ViewModel() {

    var aliceKeyPair by mutableStateOf<KeyPair?>(null)
        private set
    var bobKeyPair by mutableStateOf<KeyPair?>(null)
        private set
    var aliceSharedKey by mutableStateOf<SecretKey?>(null)
        private set
    var bobSharedKey by mutableStateOf<SecretKey?>(null)
        private set
    var inputText by mutableStateOf("")
    var encryptedText by mutableStateOf("")
        private set
    var decryptedText by mutableStateOf("")
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    fun generateKeys() {
        aliceKeyPair = EcdhKeyExchange.generateKeyPair()
        bobKeyPair = EcdhKeyExchange.generateKeyPair()
        aliceSharedKey = null
        bobSharedKey = null
    }

    fun deriveSharedKeys() {
        val alice = aliceKeyPair ?: return
        val bob = bobKeyPair ?: return
        aliceSharedKey = EcdhKeyExchange.deriveSharedAesKey(alice.private, bob.public)
        bobSharedKey = EcdhKeyExchange.deriveSharedAesKey(bob.private, alice.public)
    }

    fun aliceEncrypts() {
        errorText = null
        encryptedText = try {
            val key = aliceSharedKey ?: error("Сначала согласуй ключи")
            AesCipher.encrypt(inputText, key)
        } catch (e: Exception) {
            errorText = "Ошибка: ${e.message}"
            ""
        }
    }

    fun bobDecrypts() {
        errorText = null
        decryptedText = try {
            val key = bobSharedKey ?: error("Сначала согласуй ключи")
            AesCipher.decrypt(encryptedText, key)
        } catch (e: Exception) {
            errorText = "Ошибка: ${e.message}"
            ""
        }
    }
}
