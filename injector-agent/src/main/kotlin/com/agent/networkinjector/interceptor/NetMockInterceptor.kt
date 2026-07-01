package com.agent.networkinjector.interceptor

import com.agent.networkinjector.processor.NetMockConfig
import com.agent.networkinjector.server.NetMockServer
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

class NetMockInterceptor : Interceptor {

    init {
        NetMockServer.start()
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val urlString = request.url.toString()

        val isGraphQL = urlString.contains("/graphql", ignoreCase = true)
        val currentMode = NetMockConfig.targetMode
        val pattern = NetMockConfig.targetPattern

        var shouldApplyRules = false

        when (currentMode) {
            "GLOBAL" -> {
                shouldApplyRules = true
            }

            "REST" -> {
                if (!isGraphQL) {
                    shouldApplyRules =
                        pattern.isBlank() || urlString.contains(pattern, ignoreCase = true)
                }
            }

            "GRAPHQL" -> {
                if (isGraphQL) {
                    if (pattern.isBlank()) {
                        shouldApplyRules = true
                    } else {
                        val bodyString = readRequestBody(request)
                        val operationName = extractGraphQLOperationName(bodyString)
                        shouldApplyRules = operationName.equals(pattern, ignoreCase = true)
                    }
                }
            }
        }

        // 2. Apply Interception Logic
        if (shouldApplyRules) {
            val delay = NetMockConfig.latencyMs
            if (delay > 0) {
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }

            if (pattern.contains("TIMEOUT", ignoreCase = true)) {
                throw IOException("NetMock Simulated Socket Network Timeout Failure Exception")
            }

            val forcedErrorCode = extractErrorCode(pattern)
            if (forcedErrorCode != null) {
                return createMockErrorResponse(request, forcedErrorCode, isGraphQL)
            }
        }

        return chain.proceed(request)
    }

    private fun readRequestBody(request: okhttp3.Request): String {
        return try {
            val copy = request.newBuilder().build()
            val buffer = Buffer()
            copy.body?.writeTo(buffer)
            buffer.readString(Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Extracts operational names directly from standard raw schema JSON request payloads.
     */
    private fun extractGraphQLOperationName(jsonBody: String): String {
        val pattern = "\"operationName\"\\s*:\\s*\"?([^\",}]+)\"?".toRegex()
        val matchResult = pattern.find(jsonBody)
        return matchResult?.groupValues?.get(1)?.trim() ?: ""
    }

    /**
     * Checks if the user specified an explicit status code integer in the target field (e.g., "500").
     */
    private fun extractErrorCode(pattern: String): Int? {
        val digits = pattern.filter { it.isDigit() }
        if (digits.length == 3) {
            return digits.toIntOrNull()
        }
        return null
    }

    /**
     * Constructs a synthetic synthetic response frame payload on the fly to bypass server processing.
     */
    private fun createMockErrorResponse(
        request: okhttp3.Request,
        statusCode: Int,
        isGraphQL: Boolean,
    ): Response {
        val contentType = "application/json; charset=utf-8".toMediaTypeOrNull()

        val mockResponseBody = if (isGraphQL) {
            """{"errors":[{"message":"NetMock Simulated GraphQL Error Event Payload Intercepted","extensions":{"code":"INTERNAL_SERVER_ERROR"}}]}"""
        } else {
            """{"status":"error","message":"NetMock Forced Server Connectivity Simulation Failure Exception"}"""
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_2)
            .code(if (isGraphQL) 200 else statusCode) // GraphQL standard maps exceptions inside 200 envelopes
            .message(if (isGraphQL) "OK" else "NetMock Injected Error")
            .body(mockResponseBody.toResponseBody(contentType))
            .build()
    }
}
