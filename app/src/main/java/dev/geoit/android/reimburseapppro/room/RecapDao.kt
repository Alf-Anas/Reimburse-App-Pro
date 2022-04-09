package dev.geoit.android.reimburseapppro.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecapDao {
    @Insert
    fun insert(recapEntity: RecapEntity)

    @Query("DELETE FROM `TABLE-RECAPITULATION` WHERE ID_Project==:idProject AND ID==:id")
    fun deleteRecap(idProject: String, id: Int): Int

    @Query("SELECT * FROM `TABLE-RECAPITULATION`")
    fun getAllRecap(): List<RecapEntity>

    @Query("SELECT * FROM `TABLE-RECAPITULATION` WHERE ID_Project==:idProject")
    fun getProjectRecap(idProject: String): List<RecapEntity>

    @Query("DELETE FROM `TABLE-RECAPITULATION` WHERE ID_Project==:idProject")
    fun deleteAllRecapByIDProject(idProject: String): Int


}