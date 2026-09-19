package com.cipherforge.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cipherforge.crypto.AesCipher
import com.cipherforge.crypto.ClassicCiphers
import javax.crypto.SecretKey

enum class CipherType(val label: String) {
    CAESAR("Цезарь"),
    VIGENERE("Виженер"),
    AES("AES-256-GCM")
}

/**
 * Хранит состояние экрана "Классика / AES" и содержит всю логику шифрования.
 * Экран (Composable) только читает состояние и вызывает методы — сам не думает.
 */
class CipherViewModel : ViewModel() {

    var selectedCipher by mutableStateOf(CipherType.CAESAR)
        private set
    var inputText by mutableStateOf("")
    var paramText by mutableStateOf("") // сдвиг для Цезаря / ключевое слово для Виженера
    var aesKey by mutableStateOf<SecretKey?>(null)
        private set
    var outputText by mutableStateOf("")
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    fun selectCipher(type: CipherType) {
        selectedCipher = type
        errorText = null
        outputText = ""
    }

    fun generateAesKey() {
        aesKey = AesCipher.generateKey()
    }

    fun setAesKeyFromInput(base64: String) {
        aesKey = try {
            AesCipher.keyFromString(base64)
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt() = runOperation {
        when (selectedCipher) {
            CipherType.CAESAR -> ClassicCiphers.caesarEncrypt(inputText, paramText.toIntOrNull() ?: 0)
            CipherType.VIGENERE -> ClassicCiphers.vigenereEncrypt(inputText, paramText)
            CipherType.AES -> {
                val key = aesKey ?: error("Сначала сгенерируй или введи ключ")
                AesCipher.encrypt(inputText, key)
            }
        }
    }

    fun decrypt() = runOperation {
        when (selectedCipher) {
            CipherType.CAESAR -> ClassicCiphers.caesarDecrypt(inputText, paramText.toIntOrNull() ?: 0)
            CipherType.VIGENERE -> ClassicCiphers.vigenereDecrypt(inputText, paramText)
            CipherType.AES -> {
                val key = aesKey ?: error("Введи ключ, которым было зашифровано")
                AesCipher.decrypt(inputText, key)
            }
        }
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
