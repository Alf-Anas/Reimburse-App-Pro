package dev.geoit.android.reimburseapppro.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProjectDao {
    @Insert
    fun insert(projectEntity: ProjectEntity)

    @Query("DELETE FROM `TABLE-PROJECT` WHERE ID_Project==:idProject")
    fun deleteProjectByIDProject(idProject: String): Int

    @Query("SELECT * FROM `TABLE-PROJECT`")
    fun getAllProject(): List<ProjectEntity>
}