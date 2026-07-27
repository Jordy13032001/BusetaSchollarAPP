package com.example.busetaescolarapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PagoDao {

    @Query("SELECT * FROM pagos WHERE correoPadre = :correoPadre ORDER BY fechaHora DESC")
    fun getByPadre(correoPadre: String): LiveData<List<PagoEntity>>

    // Se usa para saber si el cupo de ese hijo ya está pagado
    @Query("SELECT * FROM pagos WHERE idEstudiante = :idEstudiante ORDER BY fechaHora DESC LIMIT 1")
    suspend fun getUltimoPagoDeEstudiante(idEstudiante: Int): PagoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(pago: PagoEntity): Long
}
