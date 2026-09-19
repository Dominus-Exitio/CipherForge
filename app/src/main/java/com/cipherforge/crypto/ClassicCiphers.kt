package com.cipherforge.crypto

/**
 * Классические шифры — обучающий режим.
 * Логика перенесена из проверенного прототипа CipherCore.kt без изменений.
 */
object ClassicCiphers {

    fun caesarEncrypt(text: String, shift: Int): String = caesarShift(text, shift)
    fun caesarDecrypt(text: String, shift: Int): String = caesarShift(text, -shift)

    private fun caesarShift(text: String, shift: Int): String {
        val s = ((shift % 26) + 26) % 26
        return text.map { ch ->
            when {
                ch in 'a'..'z' -> 'a' + (ch - 'a' + s) % 26
                ch in 'A'..'Z' -> 'A' + (ch - 'A' + s) % 26
                ch in 'а'..'я' -> 'а' + (ch - 'а' + s) % 32
                ch in 'А'..'Я' -> 'А' + (ch - 'А' + s) % 32
                else -> ch
            }
        }.joinToString("")
    }

    fun vigenereEncrypt(text: String, key: String): String = vigenere(text, key, encrypt = true)
    fun vigenereDecrypt(text: String, key: String): String = vigenere(text, key, encrypt = false)

    private fun vigenere(text: String, key: String, encrypt: Boolean): String {
        require(key.isNotEmpty()) { "Ключ не может быть пустым" }
        val cleanKey = key.lowercase().filter { it in 'a'..'z' }
        require(cleanKey.isNotEmpty()) { "Ключ должен содержать латинские буквы" }

        var ki = 0
        return text.map { ch ->
            if (ch.isLetter()) {
                val shift = cleanKey[ki % cleanKey.length] - 'a'
                ki++
                val realShift = if (encrypt) shift else -shift
                when {
                    ch in 'a'..'z' -> 'a' + (((ch - 'a' + realShift) % 26 + 26) % 26)
                    ch in 'A'..'Z' -> 'A' + (((ch - 'A' + realShift) % 26 + 26) % 26)
                    else -> ch
                }
            } else ch
        }.joinToString("")
    }
}
