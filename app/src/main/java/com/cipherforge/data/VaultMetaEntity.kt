package com.cipherforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Единственная строка с солью для PBKDF2. Соль — не секрет (её назначение — защита
 * от rainbow-table атак, а не секретность сама по себе), поэтому хранить её в открытом
 * виде в базе нормально. Без неё при перезапуске приложения тот же пароль давал бы
 * другой ключ и старые заметки перестали бы расшифровываться.
 */
@Entity(tableName = "vault_meta")
data class VaultMetaEntity(
    @PrimaryKey val id: Int = 0,
    val saltBase64: String
)
