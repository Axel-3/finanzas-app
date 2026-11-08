package com.axel.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface ProcessedNotificationKeyDao {
    /**
     * El índice único de la tabla + `OnConflictStrategy.IGNORE` hacen el
     * dedup atómico: Room devuelve -1 cuando la fila ya existía (no
     * insertó nada). El llamador DEBE revisar el valor de retorno y
     * abortar si es -1 — un `exists()` seguido de un `insert()` por
     * separado (como estaba antes) deja una ventana de carrera: si
     * Android dispara `onNotificationPosted` dos veces casi al mismo
     * tiempo para la misma notificación, las dos corutinas pueden ver
     * "no existe" antes de que cualquiera termine de insertar, y las dos
     * procesan — confirmado en dispositivo real con un cargo duplicado
     * en "Por revisar" con el mismo milisegundo exacto.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: com.axel.finanzas.core.database.entity.ProcessedNotificationKeyEntity): Long
}
