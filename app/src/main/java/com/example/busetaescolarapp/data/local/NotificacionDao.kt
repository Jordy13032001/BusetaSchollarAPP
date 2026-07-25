package com.example.busetaescolarapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificacionDao {

    @Query("SELECT * FROM notificaciones WHERE correoPadre = :correoPadre ORDER BY id DESC")
    fun getByPadre(correoPadre: String): LiveData<List<NotificacionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notificaciones: List<NotificacionEntity>)

    @Query("DELETE FROM notificaciones WHERE correoPadre = :correoPadre")
    suspend fun deleteByPadre(correoPadre: String)
}
