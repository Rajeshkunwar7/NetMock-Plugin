package com.plugin.networkinjector.adb

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import com.intellij.openapi.diagnostic.Logger

class AdbBridgeManager {

    private val logger = Logger.getInstance(AdbBridgeManager::class.java)
    private val defaultPort = 8888

    /**
     * Resolves the local machine system path to the Android SDK platform-tools 'adb' binary.
     */
    private fun findAdbBinary(): String {
        val userHome = System.getProperty("user.home")
        // Check standard macOS path location
        val macOSAdb = File("$userHome/Library/Android/sdk/platform-tools/adb")
        if (macOSAdb.exists()) return macOSAdb.absolutePath

        // Fallback check for standard Windows path location
        val windowsAdb = File("$userHome\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe")
        if (windowsAdb.exists()) return windowsAdb.absolutePath

        return "adb" // Fallback to system environment variable path lookup
    }

    /**
     * Executes a background terminal shell command line string and reads the textual output response.
     */
    private fun executeCommand(command: String): List<String> {
        val outputLines = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec(command)
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { outputLines.add(it) }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            logger.error("Failed to execute system shell instruction command: $command", e)
        }
        return outputLines
    }

    /**
     * Queries connected devices using 'adb devices' and returns a list of active hardware serial identifiers.
     */
    fun getConnectedDevices(): List<String> {
        val adbPath = findAdbBinary()
        val commandOutput = executeCommand("$adbPath devices")
        val deviceList = mutableListOf<String>()

        for (line in commandOutput) {
            if (line.startsWith("List of devices") || line.isBlank()) continue
            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 2 && parts[1] == "device") {
                deviceList.add(parts[0]) // Capture serial/identifier string token
            }
        }
        return deviceList
    }

    /**
     * Forwards a local computer TCP socket port over the ADB pipeline interface bridge to the target runtime app instance.
     */
    fun setupPortForwarding(deviceSerial: String? = null): Boolean {
        val adbPath = findAdbBinary()
        val devicePrefix = if (!deviceSerial.isNullOrBlank()) "-s $deviceSerial " else ""
        val command = "$adbPath ${devicePrefix}forward tcp:$defaultPort tcp:$defaultPort"

        executeCommand(command)
        // Verify forwarding rule successfully registered
        val listForwarding = executeCommand("$adbPath forward --list")
        return listForwarding.any { it.contains("tcp:$defaultPort") }
    }

    /**
     * Broadcasts a text configuration data frame packet directly down the active ADB socket connection tunnel.
     */
    fun sendConfigurationUpdate(jsonPayload: String): Boolean {
        return try {
            // Establish a low-latency connection pipeline over local loopback interface
            Socket("127.0.0.1", defaultPort).use { socket ->
                socket.soTimeout = 2000 // Block infinite hangs if the application is suspended
                OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8).use { writer ->
                    writer.write(jsonPayload)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            logger.warn("Unable to broadcast packet configuration over local ADB port tunnel connection: ${e.message}")
            false
        }
    }
}
