package com.tishou.assistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tishou.assistant.R
import com.tishou.assistant.model.GrabLog

/**
 * 日志列表适配器
 * 
 * 用于在 RecyclerView 中显示抢单日志
 */
class LogAdapter(
    private val onItemClick: (GrabLog) -> Unit
) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val logs = mutableListOf<GrabLog>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position], onItemClick)
    }

    override fun getItemCount(): Int = logs.size

    /**
     * 提交新的日志列表
     */
    fun submitList(newLogs: List<GrabLog>) {
        logs.clear()
        logs.addAll(newLogs)
        notifyDataSetChanged()
    }

    /**
     * 添加单条日志
     */
    fun addLog(log: GrabLog) {
        logs.add(0, log)
        notifyItemInserted(0)
    }

    /**
     * 清除所有日志
     */
    fun clear() {
        val size = logs.size
        logs.clear()
        notifyItemRangeRemoved(0, size)
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvResult: TextView = itemView.findViewById(R.id.tvResult)
        private val tvOrderNo: TextView = itemView.findViewById(R.id.tvOrderNo)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvResponseTime: TextView = itemView.findViewById(R.id.tvResponseTime)

        fun bind(log: GrabLog, onItemClick: (GrabLog) -> Unit) {
            // 格式化时间
            val timeFormat = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
            tvTime.text = timeFormat.format(java.util.Date(log.timestamp))

            // 设置结果文本和颜色
            if (log.isSuccess) {
                tvResult.text = "成功"
                tvResult.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_success))
            } else {
                tvResult.text = "失败"
                tvResult.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_error))
            }

            // 订单号
            tvOrderNo.text = log.orderNo.ifEmpty { "无订单号" }

            // 金额
            tvAmount.text = "¥${String.format("%.2f", log.amount)}"

            // 响应时间
            tvResponseTime.text = "${log.responseTime}ms"

            // 点击事件
            itemView.setOnClickListener {
                onItemClick(log)
            }
        }
    }
}