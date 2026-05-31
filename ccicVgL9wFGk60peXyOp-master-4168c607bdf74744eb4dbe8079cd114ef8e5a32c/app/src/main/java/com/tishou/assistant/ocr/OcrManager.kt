package com.tishou.assistant.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.tishou.assistant.core.LogLevel
import com.tishou.assistant.core.TiShouCore
import com.tishou.assistant.model.OrderInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * OCR 管理器
 * 
 * 负责：
 * 1. 截图获取与压缩
 * 2. 图像预处理（JNI 调用 C++ 优化）
 * 3. 调用 OCR API 识别
 * 4. 解析 OCR 结果并提取订单信息
 */
class OcrManager private constructor(
    private val context: Context,
    private val core: TiShouCore
) {

    companion object {
        private const val TAG = "OcrManager"
        
        @Volatile
        private var instance: OcrManager? = null
        
        fun getInstance(context: Context, core: TiShouCore): OcrManager {
            return instance ?: synchronized(this) {
                instance ?: OcrManager(context, core).also { instance = it }
            }
        }
    }

    private val apiService = OcrApiService.getInstance()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
    
    // 缓存目录
    private val cacheDir: File by lazy {
        File(context.cacheDir, "ocr_images").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 识别 Bitmap 图片
     * 
     * @param bitmap 要识别的图片
     * @param compressQuality 压缩质量 (0-100)
     * @return OCR 识别结果
     */
    suspend fun recognizeBitmap(
        bitmap: Bitmap,
        compressQuality: Int = 80
    ): OcrApiService.OcrResult {
        // 保存 Bitmap 到临时文件
        val imageFile = saveBitmapToCache(bitmap, compressQuality)
        
        if (!imageFile.exists()) {
            core.log("图片保存失败", LogLevel.ERROR)
            return OcrApiService.OcrResult(
                requestId = "",
                text = "",
                ocrCount = 0,
                created = System.currentTimeMillis(),
                success = false,
                errorMessage = "图片保存失败"
            )
        }

        core.log("开始 OCR 识别，文件大小：${imageFile.length() / 1024}KB", LogLevel.DEBUG)

        // 调用 API 识别
        val result = try {
            apiService.recognizeImage(imageFile)
        } catch (e: Exception) {
            core.log("OCR 识别异常：${e.message}", LogLevel.ERROR)
            OcrApiService.OcrResult(
                requestId = "",
                text = "",
                ocrCount = 0,
                created = System.currentTimeMillis(),
                success = false,
                errorMessage = e.message
            )
        }

        // 识别成功后删除临时文件
        if (result.success) {
            imageFile.delete()
            core.log("OCR 识别完成，文本长度：${result.text.length}", LogLevel.DEBUG)
        } else {
            core.log("OCR 识别失败：${result.errorMessage}", LogLevel.WARNING)
        }

        return result
    }

    /**
     * 识别屏幕指定区域
     * 
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @return OCR 识别结果
     */
    suspend fun recognizeScreenRegion(
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): OcrApiService.OcrResult {
        // 这里需要结合 MediaProjection 或无障碍服务获取屏幕截图
        // 简化处理，返回提示
        return OcrApiService.OcrResult(
            requestId = "",
            text = "",
            ocrCount = 0,
            created = System.currentTimeMillis(),
            success = false,
            errorMessage = "屏幕区域截图需要额外权限"
        )
    }

    /**
     * 从 OCR 结果中提取订单信息
     * 
     * @param ocrText OCR 识别的文本
     * @return 订单信息对象
     */
    fun parseOrderInfo(ocrText: String): OrderInfo {
        val orderInfo = OrderInfo()
        val lines = ocrText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        // 订单号匹配（常见格式：数字、字母数字组合）
        val orderNoPattern = Pattern.compile("(?:订单 [号号]?|单号)[:：\\s]*([A-Z0-9]{8,20})", Pattern.CASE_INSENSITIVE)
        orderNoPattern.matcher(ocrText).let { matcher ->
            if (matcher.find()) {
                orderInfo.orderNo = matcher.group(1)
            }
        }

        // 金额匹配（¥、￥、元等）
        val amountPattern = Pattern.compile("(?:¥|￥|元|价格)[:：\\s]*([0-9]+\\.?[0-9]*)")
        amountPattern.matcher(ocrText).let { matcher ->
            if (matcher.find()) {
                orderInfo.amount = matcher.group(1).toDoubleOrNull() ?: 0.0
            }
        }

        // 时间匹配
        val timePattern = Pattern.compile("(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[\\s]\\d{1,2}:\\d{1,2})")
        timePattern.matcher(ocrText).let { matcher ->
            if (matcher.find()) {
                orderInfo.time = matcher.group(1)
            }
        }

        // 发货地/目的地匹配
        val locationKeywords = listOf("发往", "目的地", "收货地", "起点", "终点")
        for (line in lines) {
            for (keyword in locationKeywords) {
                if (line.contains(keyword)) {
                    orderInfo.location = line.substringAfter(keyword).trim()
                    break
                }
            }
        }

        // 货物类型匹配
        val goodsKeywords = listOf("货物", "物品", "商品", "产品", "类型")
        for (line in lines) {
            for (keyword in goodsKeywords) {
                if (line.contains(keyword)) {
                    orderInfo.goodsType = line.substringAfter(keyword).trim()
                    break
                }
            }
        }

        // 重量匹配
        val weightPattern = Pattern.compile("([0-9]+\\.?[0-9]*)[\\s]*(吨 | 公斤 |kg|斤)")
        weightPattern.matcher(ocrText).let { matcher ->
            if (matcher.find()) {
                orderInfo.weight = matcher.group(1).toDoubleOrNull() ?: 0.0
            }
        }

        // 原始文本
        orderInfo.rawText = ocrText
        orderInfo.lines = lines

        core.log("订单信息解析完成：${orderInfo.toSummary()}", LogLevel.INFO)
        return orderInfo
    }

    /**
     * 检查文本是否匹配抢单规则
     * 
     * @param text OCR 识别的文本
     * @param keywords 关键词列表
     * @param excludeKeywords 排除关键词列表
     * @return 是否匹配
     */
    fun matchesRule(
        text: String,
        keywords: List<String>,
        excludeKeywords: List<String> = emptyList()
    ): Boolean {
        // 检查排除词
        for (exclude in excludeKeywords) {
            if (text.contains(exclude, ignoreCase = true)) {
                core.log("命中排除词：$exclude", LogLevel.DEBUG)
                return false
            }
        }

        // 检查包含词（至少匹配一个）
        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true)) {
                core.log("命中关键词：$keyword", LogLevel.DEBUG)
                return true
            }
        }

        return false
    }

    /**
     * 保存 Bitmap 到缓存目录
     */
    private fun saveBitmapToCache(bitmap: Bitmap, quality: Int): File {
        val filename = "ocr_${dateFormat.format(Date()}.jpg"
        val file = File(cacheDir, filename)

        try {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.flush()
            }
        } catch (e: Exception) {
            core.log("保存图片失败：${e.message}", LogLevel.ERROR)
        }

        return file
    }

    /**
     * 图像预处理（调用 JNI）
     * 可以进行灰度化、二值化、对比度增强等操作
     */
    fun preprocessImage(bitmap: Bitmap): Bitmap {
        // TODO: 调用 NDK 进行图像预处理
        // 目前直接返回原图
        return bitmap
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            core.log("OCR 缓存已清理", LogLevel.DEBUG)
        } catch (e: Exception) {
            core.log("清理缓存失败：${e.message}", LogLevel.WARNING)
        }
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}