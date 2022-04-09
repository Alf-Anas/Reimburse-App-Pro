package dev.geoit.android.reimburseapppro.room

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.geoit.android.reimburseapppro.room.ProjectEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @NonNull @ColumnInfo(name = "Date") val date: String,
    @NonNull @ColumnInfo(name = "ID_Project") val id_project: String,
    @NonNull @ColumnInfo(name = "Project_Name") val project_name: String,
) {
    companion object {
        const val TABLE_NAME = "table-project"
    }
}