package com.cipherforge.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cipherforge.crypto.AesCipher
import com.cipherforge.crypto.KeyDerivation
import com.cipherforge.data.AppDatabase
import com.cipherforge.data.NoteEntity
import com.cipherforge.data.VaultMetaEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.crypto.spec.SecretKeySpec

data class SecureNote(val id: Long, val title: String, val encryptedContent: String)

/**
 * Хранилище заметок, защищённое мастер-паролем.
 *
 * Заметки и соль для вывода ключа сохраняются в Room (SQLite на устройстве) —
 * переживают перезапуск приложения. Сам мастер-пароль нигде не хранится,
 * в памяти на время сессии держится только производный от него AES-ключ.
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val noteDao = db.noteDao()
    private val metaDao = db.vaultMetaDao()

    var isUnlocked by mutableStateOf(false)
        private set
    var vaultExists by mutableStateOf(false)
        private set
    var masterPasswordInput by mutableStateOf("")
    var newTitle by mutableStateOf("")
    var newContent by mutableStateOf("")
    var errorText by mutableStateOf<String?>(null)
        private set
    var revealedNoteId by mutableStateOf<Long?>(null)
        private set
    var revealedContent by mutableStateOf("")
        private set

    val notes = mutableStateListOf<SecureNote>()

    private var vaultKey: SecretKeySpec? = null
    private var salt: ByteArray? = null

    init {
        // Проверяем, создавалось ли хранилище раньше (соль уже сохранена в базе)
        viewModelScope.launch {
            metaDao.get()?.let { meta ->
                salt = KeyDerivation.saltFromString(meta.saltBase64)
                vaultExists = true
            }
        }
        // Подписка на список заметок: база — источник правды, список в UI обновляется сам
        viewModelScope.launch {
            noteDao.getAll().collectLatest { entities ->
                notes.clear()
                notes.addAll(entities.map { SecureNote(it.id, it.title, it.encryptedContent) })
            }
        }
    }

    /** Первое создание хранилища: генерируем соль, сохраняем её в базу, выводим ключ. */
    fun createVault() {
        if (masterPasswordInput.isBlank()) {
            errorText = "Введи мастер-пароль"
            return
        }
        val newSalt = KeyDerivation.generateSalt()
        salt = newSalt
        vaultKey = KeyDerivation.deriveKey(masterPasswordInput.toCharArray(), newSalt)
        isUnlocked = true
        vaultExists = true
        errorText = null
        masterPasswordInput = ""
        viewModelScope.launch {
            metaDao.insert(VaultMetaEntity(saltBase64 = KeyDerivation.saltToString(newSalt)))
        }
    }

    /** Разблокировка уже существующего (сохранённого ранее) хранилища тем же паролем. */
    fun unlock() {
        val currentSalt = salt
        if (currentSalt == null) {
            errorText = "Хранилище ещё не создано — сначала создай его"
            return
        }
        val candidateKey = KeyDerivation.deriveKey(masterPasswordInput.toCharArray(), currentSalt)
        if (notes.isNotEmpty()) {
            try {
                AesCipher.decrypt(notes.first().encryptedContent, candidateKey)
            } catch (e: Exception) {
                errorText = "Неверный пароль"
                return
            }
        }
        vaultKey = candidateKey
        isUnlocked = true
        errorText = null
        masterPasswordInput = ""
    }

    fun lock() {
        vaultKey = null
        isUnlocked = false
        revealedNoteId = null
        revealedContent = ""
    }

    fun addNote() {
        val key = vaultKey ?: return
        if (newTitle.isBlank() || newContent.isBlank()) {
            errorText = "Заполни заголовок и содержимое"
            return
        }
        val encrypted = AesCipher.encrypt(newContent, key)
        viewModelScope.launch {
            noteDao.insert(NoteEntity(title = newTitle, encryptedContent = encrypted))
        }
        newTitle = ""
        newContent = ""
        errorText = null
    }

    fun reveal(note: SecureNote) {
        val key = vaultKey ?: return
        revealedContent = try {
            AesCipher.decrypt(note.encryptedContent, key)
        } catch (e: Exception) {
            "Ошибка расшифровки"
        }
        revealedNoteId = note.id
    }

    fun hideRevealed() {
        revealedNoteId = null
        revealedContent = ""
    }
}
