package com.tishou.assistant.ocr

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * CSDN OCR API 服务
 * 
 * API 文档：
 * POST https://models.csdn.net/v1/images/ocr
 * Authorization: Bearer sk-kpoorxpbmlrrkptl
 */
class OcrApiService private constructor() {

    companion object {
        private const val TAG = "OcrApiService"
        private const val BASE_URL = "https://models.csdn.net"
        private const val API_KEY = "sk-kpoorxpbmlrrkptl"
        
        @Volatile
        private var instance: OcrApiService? = null
        
        fun getInstance(): OcrApiService {
            return instance ?: synchronized(this) {
                instance ?: OcrApiService().also { instance = it }
            }
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * OCR 识别结果
     */
    data class OcrResult(
        val requestId: String,
        val text: String,
        val ocrCount: Int,
        val created: Long,
        val success: Boolean,
        val errorMessage: String? = null
    )

    /**
     * 通过图片文件进行 OCR 识别
     * 
     * @param imageFile 图片文件
     * @param model OCR 模型名称（默认 default_ocr）
     * @return OCR 识别结果
     */
    suspend fun recognizeImage(imageFile: File, model: String = "default_ocr"): OcrResult {
        return suspendCoroutine { continuation ->
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart(
                    "image",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/v1/images/ocr")
                .addHeader("Authorization", "Bearer $API_KEY")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "OCR 请求失败", e)
                    continuation.resume(
                        OcrResult(
                            requestId = "",
                            text = "",
                            ocrCount = 0,
                            created = System.currentTimeMillis(),
                            success = false,
                            errorMessage = "网络请求失败：${e.message}"
                        )
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            continuation.resume(
                                OcrResult(
                                    requestId = "",
                                    text = "",
                                    ocrCount = 0,
                                    created = System.currentTimeMillis(),
                                    success = false,
                                    errorMessage = "HTTP 错误：${response.code}"
                                )
                            )
                            return
                        }

                        try {
                            val responseBody = response.body?.string() ?: ""
                            val json = JSONObject(responseBody)
                            
                            val result = OcrResult(
                                requestId = json.optString("request_id", ""),
                                text = json.optString("data", ""),
                                ocrCount = json.optJSONObject("usage")?.optInt("ocr_count", 1) ?: 1,
                                created = json.optLong("created", System.currentTimeMillis()),
                                success = true
                            )
                            
                            Log.d(TAG, "OCR 识别成功：${result.text.length} 字符")
                            continuation.resume(result)
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON 解析失败", e)
                            continuation.resume(
                                OcrResult(
                                    requestId = "",
                                    text = "",
                                    ocrCount = 0,
                                    created = System.currentTimeMillis(),
                                    success = false,
                                    errorMessage = "JSON 解析失败：${e.message}"
                                )
                            )
                        }
                    }
                }
            })
        }
    }

    /**
     * 通过图片 URL 进行 OCR 识别
     * 
     * @param imageUrl 图片 URL
     * @param model OCR 模型名称（默认 default_ocr）
     * @return OCR 识别结果
     */
    suspend fun recognizeUrl(imageUrl: String, model: String = "default_ocr"): OcrResult {
        return suspendCoroutine { continuation ->
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", model)
                .addFormDataPart("url", imageUrl)
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/v1/images/ocr")
                .addHeader("Authorization", "Bearer $API_KEY")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "OCR 请求失败", e)
                    continuation.resume(
                        OcrResult(
                            requestId = "",
                            text = "",
                            ocrCount = 0,
                            created = System.currentTimeMillis(),
                            success = false,
                            errorMessage = "网络请求失败：${e.message}"
                        )
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            continuation.resume(
                                OcrResult(
                                    requestId = "",
                                    text = "",
                                    ocrCount = 0,
                                    created = System.currentTimeMillis(),
                                    success = false,
                                    errorMessage = "HTTP 错误：${response.code}"
                                )
                            )
                            return
                        }

                        try {
                            val responseBody = response.body?.string() ?: ""
                            val json = JSONObject(responseBody)
                            
                            val result = OcrResult(
                                requestId = json.optString("request_id", ""),
                                text = json.optString("data", ""),
                                ocrCount = json.optJSONObject("usage")?.optInt("ocr_count", 1) ?: 1,
                                created = json.optLong("created", System.currentTimeMillis()),
                                success = true
                            )
                            
                            Log.d(TAG, "OCR 识别成功：${result.text.length} 字符")
                            continuation.resume(result)
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON 解析失败", e)
                            continuation.resume(
                                OcrResult(
                                    requestId = "",
                                    text = "",
                                    ocrCount = 0,
                                    created = System.currentTimeMillis(),
                                    success = false,
                                    errorMessage = "JSON 解析失败：${e.message}"
                                )
                            )
                        }
                    }
                }
            })
        }
    }

    /**
     * 同步方式识别（用于非协程环境）
     */
    fun recognizeImageSync(imageFile: File, model: String = "default_ocr"): OcrResult {
        val latch = CountDownLatch(1)
        var result: OcrResult? = null

        // 启动协程
        kotlinx.coroutines.GlobalScope.launch {
            result = recognizeImage(imageFile, model)
            latch.countDown()
        }

        try {
            latch.await(60, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            return OcrResult("", "", 0, System.currentTimeMillis(), false, "等待超时")
        }

        return result ?: OcrResult("", "", 0, System.currentTimeMillis(), false, "结果为空")
    }
}