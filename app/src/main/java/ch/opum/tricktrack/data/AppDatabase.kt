package ch.opum.tricktrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.opum.tricktrack.data.place.SavedPlace
import ch.opum.tricktrack.data.place.SavedPlaceDao

@Database(entities = [Trip::class, SavedPlace::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun savedPlaceDao(): SavedPlaceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) { // Changed 'database' to 'db'
                db.execSQL("ALTER TABLE trips ADD COLUMN endDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE trips SET endDate = date")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trip_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true) // Replaced deprecated method with explicit parameter
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
