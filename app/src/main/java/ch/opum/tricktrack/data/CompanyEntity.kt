package ch.opum.tricktrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)
