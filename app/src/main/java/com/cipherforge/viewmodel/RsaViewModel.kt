package com.cipherforge.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cipherforge.crypto.RsaCipher
import java.security.KeyPair

class RsaViewModel : ViewModel() {

    var keyPair by mutableStateOf<KeyPair?>(null)
        private set
    var inputText by mutableStateOf("")
    var outputText by mutableStateOf("")
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    fun generateKeys() {
        keyPair = RsaCipher.generateKeyPair()
    }

    fun encrypt() = runOperation {
        val kp = keyPair ?: error("Сначала сгенерируй ключи")
        RsaCipher.encryptHybrid(inputText, kp.public)
    }

    fun decrypt() = runOperation {
        val kp = keyPair ?: error("Сначала сгенерируй ключи")
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
