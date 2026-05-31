package com.tishou.assistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.tishou.assistant.model.GrabLog

/**
 * 日志数据访问对象
 */
@Dao
interface LogDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: GrabLog): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<GrabLog>)
    
    @Query("SELECT * FROM grab_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<GrabLog>
    
    @Query("SELECT * FROM grab_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 100): List<GrabLog>
    
    @Query("SELECT * FROM grab_logs WHERE isSuccess = 1 ORDER BY timestamp DESC")
    suspend fun getSuccessLogs(): List<GrabLog>
    
    @Query("SELECT * FROM grab_logs WHERE isSuccess = 0 ORDER BY timestamp DESC")
    suspend fun getFailLogs(): List<GrabLog>
    
    @Query("SELECT * FROM grab_logs WHERE orderNo = :orderNo")
    suspend fun getLogByOrderNo(orderNo: String): GrabLog?
    
    @Query("SELECT COUNT(*) FROM grab_logs WHERE isSuccess = 1 AND timestamp >= :startTime")
    suspend fun getTodaySuccessCount(startTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM grab_logs WHERE timestamp >= :startTime")
    suspend fun getTodayTotalCount(startTime: Long): Int
    
    @Query("SELECT AVG(responseTime) FROM grab_logs WHERE timestamp >= :startTime")
    suspend fun getTodayAvgResponseTime(startTime: Long): Long?
    
    @Query("DELETE FROM grab_logs")
    suspend fun deleteAllLogs()
    
    @Query("DELETE FROM grab_logs WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

/**
 * 日志 Room 数据库
 */
@Database(entities = [GrabLog::class], version = 1, exportSchema = false)
abstract class LogDatabase : RoomDatabase() {
    
    abstract fun logDao(): LogDao
    
    companion object {
        @Volatile
        private var INSTANCE: LogDatabase? = null
        
        fun getInstance(context: Context): LogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "tishou_log_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}