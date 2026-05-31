package com.tishou.assistant.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tishou.assistant.R
import com.tishou.assistant.model.GrabLog
import com.tishou.assistant.utils.LogDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日志查看界面
 * 显示抢单记录、运行日志，支持导出和筛选
 */
class LogActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabExport: FloatingActionButton
    private lateinit var logAdapter: LogAdapter
    private lateinit var logDatabase: LogDatabase

    private var currentFilter = LogFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        initViews()
        initDatabase()
        setupRecyclerView()
        setupToolbar()
        setupFab()
        loadLogs()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        fabExport = findViewById(R.id.fabExport)
    }

    private fun initDatabase() {
        logDatabase = LogDatabase.getInstance(this)
    }

    private fun setupRecyclerView() {
        logAdapter = LogAdapter { log -> showLogDetail(log) }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LogActivity)
            adapter = logAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "抢单日志"
        }
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupFab() {
        fabExport.setOnClickListener { exportLogs() }
    }

    private fun loadLogs() {
        Thread {
            val logs = when (currentFilter) {
                LogFilter.ALL -> logDatabase.logDao().getAllLogs()
                LogFilter.SUCCESS -> logDatabase.logDao().getSuccessLogs()
                LogFilter.FAIL -> logDatabase.logDao().getFailLogs()
            }
            runOnUiThread {
                logAdapter.submitList(logs)
            }
        }.start()
    }

    private fun showLogDetail(log: GrabLog) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val timeStr = dateFormat.format(Date(log.timestamp))

        val message = buildString {
            appendLine("时间：$timeStr")
            appendLine("结果：${if (log.isSuccess) "✅ 成功" else "❌ 失败"}")
            appendLine("订单号：${log.orderNo}")
            appendLine("金额：¥${log.amount}")
            appendLine("响应时间：${log.responseTime}ms")
            appendLine("备注：${log.note}")
        }

        AlertDialog.Builder(this)
            .setTitle("日志详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun exportLogs() {
        Thread {
            try {
                val logs = logDatabase.logDao().getAllLogs()
                val csvBuilder = StringBuilder()
                csvBuilder.appendLine("时间，结果，订单号，金额，响应时间 (ms),备注")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                logs.forEach { log ->
                    val timeStr = dateFormat.format(Date(log.timestamp))
                    val result = if (log.isSuccess) "成功" else "失败"
                    csvBuilder.appendLine("$timeStr,$result,${log.orderNo},${log.amount},${log.responseTime},${log.note}")
                }

                val exportDir = File(getExternalFilesDir(null), "exports")
                exportDir.mkdirs()
                val timestamp = System.currentTimeMillis()
                val exportFile = File(exportDir, "grab_logs_$timestamp.csv")
                exportFile.writeText(csvBuilder.toString())

                // 分享文件
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", exportFile)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "导出日志"))

                runOnUiThread {
                    Toast.makeText(this, "日志导出成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_log, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter_all -> {
                currentFilter = LogFilter.ALL
                loadLogs()
                true
            }
            R.id.action_filter_success -> {
                currentFilter = LogFilter.SUCCESS
                loadLogs()
                true
            }
            R.id.action_filter_fail -> {
                currentFilter = LogFilter.FAIL
                loadLogs()
                true
            }
            R.id.action_clear_logs -> {
                showClearConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showClearConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认清除")
            .setMessage("确定要清除所有日志记录吗？此操作不可恢复。")
            .setPositiveButton("清除") { _, _ ->
                Thread {
                    logDatabase.logDao().deleteAllLogs()
                    runOnUiThread {
                        loadLogs()
                        Toast.makeText(this, "日志已清除", Toast.LENGTH_SHORT).show()
                    }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    enum class LogFilter {
        ALL, SUCCESS, FAIL
    }
}