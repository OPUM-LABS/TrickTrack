package ch.opum.tricktrack.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "drivers")
data class DriverEntity(
    @SerializedName(value = "id", alternate = ["a"])
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @SerializedName(value = "name", alternate = ["b"])
    val name: String
)
