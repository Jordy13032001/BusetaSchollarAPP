package com.example.busetaescolarapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IncidenteDao {

    @Query("SELECT * FROM incidentes WHERE correoPadre = :correoPadre ORDER BY idIncidente DESC")
    fun getByPadre(correoPadre: String): LiveData<List<IncidenteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidentes: List<IncidenteEntity>)

    @Query("DELETE FROM incidentes WHERE correoPadre = :correoPadre")
    suspend fun deleteByPadre(correoPadre: String)
}
