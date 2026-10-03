package com.cipherforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Заметка хранится в базе уже зашифрованной — plain-текст в SQLite никогда не попадает. */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val encryptedContent: String
)
