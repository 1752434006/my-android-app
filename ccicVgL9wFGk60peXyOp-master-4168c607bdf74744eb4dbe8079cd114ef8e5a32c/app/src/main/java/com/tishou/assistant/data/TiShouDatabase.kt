package com.tishou.assistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

/**
 * 抢单记录实体
 */
@Entity(tableName = "grab_records")
data class GrabRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
    val orderTitle: String,
    val orderPrice: String,
    val orderLocation: String,
    val grabTime: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val responseTimeMs: Long,
    val screenshotPath: String? = null
)

/**
 * 配置项实体
 */
@Entity(tableName = "configurations")
data class Configuration(
    @PrimaryKey val key: String,
    val value: String,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * 数据访问对象 - 抢单记录
 */
@Dao
interface RecordDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GrabRecord): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<GrabRecord>)
    
    @Query("SELECT * FROM grab_records ORDER BY grabTime DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 100): Flow<List<GrabRecord>>
    
    @Query("SELECT * FROM grab_records ORDER BY grabTime DESC")
    suspend fun getAllRecords(): List<GrabRecord>
    
    @Query("SELECT COUNT(*) FROM grab_records WHERE isSuccess = 1 AND grabTime >= :startTime")
    suspend fun getTodaySuccessCount(startTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM grab_records WHERE grabTime >= :startTime")
    suspend fun getTodayTotalCount(startTime: Long): Int
    
    @Query("SELECT AVG(responseTimeMs) FROM grab_records WHERE grabTime >= :startTime")
    suspend fun getTodayAvgResponseTime(startTime: Long): Long?
    
    @Query("SELECT * FROM grab_records WHERE orderId = :orderId")
    suspend fun getRecordByOrderId(orderId: String): GrabRecord?
    
    @Query("DELETE FROM grab_records")
    suspend fun deleteAll()
    
    @Query("DELETE FROM grab_records WHERE grabTime < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

/**
 * 数据访问对象 - 配置
 */
@Dao
interface ConfigDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: Configuration)
    
    @Query("SELECT * FROM configurations WHERE key = :key")
    suspend fun getConfigByKey(key: String): Configuration?
    
    @Query("SELECT * FROM configurations")
    suspend fun getAllConfigs(): List<Configuration>
    
    @Query("DELETE FROM configurations")
    suspend fun deleteAll()
}

/**
 * Room 数据库
 */
@Database(entities = [GrabRecord::class, Configuration::class], version = 1, exportSchema = false)
abstract class TiShouDatabase : RoomDatabase() {
    
    abstract fun recordDao(): RecordDao
    abstract fun configDao(): ConfigDao
    
    companion object {
        @Volatile
        private var INSTANCE: TiShouDatabase? = null
        
        fun getInstance(context: Context): TiShouDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TiShouDatabase::class.java,
                    "tishou_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}