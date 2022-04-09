package dev.geoit.android.reimburseapppro.room

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.geoit.android.reimburseapppro.room.RecapEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class RecapEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @NonNull @ColumnInfo(name = "Date") val date: String,
    @NonNull @ColumnInfo(name = "ID_Project") val id_project: String,
    @NonNull @ColumnInfo(name = "Type") val type: String,
    @ColumnInfo(name = "Description") val description: String,
    @ColumnInfo(name = "Receipt") val receipt: String,
    @ColumnInfo(name = "Comment") val comment: String,
    @ColumnInfo(name = "Income") val income: Double,
    @ColumnInfo(name = "Expense") val expense: Double,
    @ColumnInfo(name = "Photo") val photo: String,
) {
    companion object {
        const val TABLE_NAME = "table-recapitulation"
    }
}