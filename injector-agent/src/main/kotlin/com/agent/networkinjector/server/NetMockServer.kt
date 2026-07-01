package com.agent.networkinjector.server

import com.agent.networkinjector.processor.NetMockConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object NetMockServer {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private const val PORT = 8888

    fun start() {
        if (isRunning) return
        isRunning = true

        thread(start = true, name = "NetMock-Server-Thread") {
            try {
                serverSocket = ServerSocket(PORT)
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleClientConnection(clientSocket)
                }
            } catch (_: Exception) {
            }
        }
    }


    private fun handleClientConnection(socket: Socket) {
        thread(start = true) {
            try {
                socket.use { s ->
                    val reader =
                        BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                    val payloadBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        payloadBuilder.append(line)
                    }

                    val rawJson = payloadBuilder.toString()
                    if (rawJson.isNotBlank()) {
                        parseAndApplyConfig(rawJson)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun parseAndApplyConfig(json: String) {
        try {
            val latency = extractJsonValue(json, "latencyMs")?.toLongOrNull() ?: 0L
            val mode = extractJsonValue(json, "targetMode") ?: "GLOBAL"
            val pattern = extractJsonValue(json, "targetPattern") ?: ""

            NetMockConfig.latencyMs = latency
            NetMockConfig.targetMode = mode
            NetMockConfig.targetPattern = pattern
        } catch (_: Exception) {
        }
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"?([^\",}]+)\"?".toRegex()
        val matchResult = pattern.find(json)
        return matchResult?.groupValues?.get(1)?.trim()
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
    }
}
