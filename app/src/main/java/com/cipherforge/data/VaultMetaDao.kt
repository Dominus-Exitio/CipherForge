package com.cipherforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VaultMetaDao {
    @Query("SELECT * FROM vault_meta WHERE id = 0 LIMIT 1")
    suspend fun get(): VaultMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meta: VaultMetaEntity)
}
