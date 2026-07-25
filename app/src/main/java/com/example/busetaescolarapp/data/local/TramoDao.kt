package com.example.busetaescolarapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TramoDao {

    @Query("SELECT * FROM tramo_ruta WHERE choferEmail = :choferEmail AND fecha = :fecha ORDER BY orden ASC")
    suspend fun getTramosDelDia(choferEmail: String, fecha: String): List<TramoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tramos: List<TramoEntity>)

    @Query("DELETE FROM tramo_ruta WHERE choferEmail = :choferEmail AND fecha = :fecha")
    suspend fun deleteDelDia(choferEmail: String, fecha: String)
}
