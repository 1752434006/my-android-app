package com.tishou.assistant.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tishou.assistant.R
import com.tishou.assistant.model.GrabRule
import java.util.UUID

/**
 * 订单规则配置界面
 * 
 * 用于配置抢单关键词、价格区间、区域筛选等规则
 */
class RuleConfigActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var ruleAdapter: RuleAdapter
    private lateinit var fabAdd: FloatingActionButton
    
    private val rules = mutableListOf<GrabRule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_config)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "抢单规则"
        
        initViews()
        loadRules()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        ruleAdapter = RuleAdapter(rules) { rule, position ->
            showEditDialog(rule, position)
        }
        recyclerView.adapter = ruleAdapter
        
        fabAdd.setOnClickListener {
            showAddDialog()
        }
    }

    private fun loadRules() {
        // 从 SharedPreferences 加载规则
        val prefs = getSharedPreferences("grab_rules", MODE_PRIVATE)
        val rulesJson = prefs.getString("rules", null)
        
        if (rulesJson != null) {
            // 解析 JSON（简化处理，实际应使用 Gson/Moshi）
            // 这里添加示例规则
            rules.add(GrabRule(
                id = UUID.randomUUID().toString(),
                name = "高价订单",
                keywords = listOf("急单", "加急", "高价"),
                minPrice = 100.0,
                maxPrice = Double.MAX_VALUE,
                enabled = true
            ))
            rules.add(GrabRule(
                id = UUID.randomUUID().toString(),
                name = "附近订单",
                keywords = listOf("附近", "同城"),
                minPrice = 0.0,
                maxPrice = Double.MAX_VALUE,
                enabled = true
            ))
            ruleAdapter.notifyDataSetChanged()
        }
    }

    private fun saveRules() {
        val prefs = getSharedPreferences("grab_rules", MODE_PRIVATE).edit()
        // 保存规则到 SharedPreferences（简化处理）
        prefs.apply()
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_rule, null)
        val etName = view.findViewById<EditText>(R.id.etRuleName)
        val etKeywords = view.findViewById<EditText>(R.id.etKeywords)
        val etMinPrice = view.findViewById<EditText>(R.id.etMinPrice)
        val etMaxPrice = view.findViewById<EditText>(R.id.etMaxPrice)
        
        AlertDialog.Builder(this)
            .setTitle("添加规则")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val keywords = etKeywords.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val minPrice = etMinPrice.text.toString().toDoubleOrNull() ?: 0.0
                val maxPrice = etMaxPrice.text.toString().toDoubleOrNull() ?: Double.MAX_VALUE
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "请输入规则名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val rule = GrabRule(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    keywords = keywords,
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    enabled = true
                )
                
                rules.add(rule)
                ruleAdapter.notifyItemInserted(rules.size - 1)
                saveRules()
                Toast.makeText(this, "规则已添加", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditDialog(rule: GrabRule, position: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_rule, null)
        val etName = view.findViewById<EditText>(R.id.etRuleName)
        val etKeywords = view.findViewById<EditText>(R.id.etKeywords)
        val etMinPrice = view.findViewById<EditText>(R.id.etMinPrice)
        val etMaxPrice = view.findViewById<EditText>(R.id.etMaxPrice)
        
        etName.setText(rule.name)
        etKeywords.setText(rule.keywords.joinToString(", "))
        etMinPrice.setText(rule.minPrice.toString())
        etMaxPrice.setText(if (rule.maxPrice == Double.MAX_VALUE) "" else rule.maxPrice.toString())
        
        AlertDialog.Builder(this)
            .setTitle("编辑规则")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val keywords = etKeywords.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val minPrice = etMinPrice.text.toString().toDoubleOrNull() ?: 0.0
                val maxPrice = etMaxPrice.text.toString().toDoubleOrNull() ?: Double.MAX_VALUE
                
                if (name.isEmpty()) {
                    Toast.makeText(this, "请输入规则名称", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                rules[position] = rule.copy(
                    name = name,
                    keywords = keywords,
                    minPrice = minPrice,
                    maxPrice = maxPrice
                )
                ruleAdapter.notifyItemChanged(position)
                saveRules()
                Toast.makeText(this, "规则已更新", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("删除") { _, _ ->
                rules.removeAt(position)
                ruleAdapter.notifyItemRemoved(position)
                saveRules()
                Toast.makeText(this, "规则已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 规则适配器
     */
    inner class RuleAdapter(
        private val rules: List<GrabRule>,
        private val onItemClick: (GrabRule, Int) -> Unit
    ) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

        inner class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.cardView)
            private val tvName: android.widget.TextView = itemView.findViewById(R.id.tvRuleName)
            private val tvKeywords: android.widget.TextView = itemView.findViewById(R.id.tvKeywords)
            private val tvPriceRange: android.widget.TextView = itemView.findViewById(R.id.tvPriceRange)
            private val switchEnabled: Switch = itemView.findViewById(R.id.switchEnabled)

            fun bind(rule: GrabRule, position: Int) {
                tvName.text = rule.name
                tvKeywords.text = "关键词：${rule.keywords.joinToString(", ")}"
                tvPriceRange.text = buildString {
                    append("价格：${rule.minPrice}")
                    if (rule.maxPrice != Double.MAX_VALUE) {
                        append(" - ${rule.maxPrice}")
                    }
                }
                
                switchEnabled.isChecked = rule.enabled
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    rules[position].enabled = isChecked
                    saveRules()
                }
                
                itemView.setOnClickListener {
                    onItemClick(rule, position)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_rule, parent, false)
            return RuleViewHolder(view)
        }

        override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
            holder.bind(rules[position], position)
        }

        override fun getItemCount(): Int = rules.size
    }
}