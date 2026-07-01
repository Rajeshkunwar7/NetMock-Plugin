package com.plugin.networkinjector.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.content.ContentFactory
import com.plugin.networkinjector.adb.AdbBridgeManager
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JSeparator
import javax.swing.JTextField

class NetMockToolWindowFactory : ToolWindowFactory {

    private val adbManager = AdbBridgeManager()
    private var isBridgeActive = false

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(8, 12, 8, 12)
            weightx = 1.0
            gridx = 0
            gridy = 0
        }

        // --- 1. Connection Header Section ---
        mainPanel.add(JBLabel("Target Device:"), gbc)
        gbc.gridy++

        val connectedDevices = adbManager.getConnectedDevices()
        val deviceOptions = if (connectedDevices.isEmpty()) {
            arrayOf("No Active Devices Detected")
        } else {
            connectedDevices.toTypedArray()
        }

        val deviceDropdown = JComboBox(deviceOptions)
        mainPanel.add(deviceDropdown, gbc)
        gbc.gridy++

        val toggleButton = JButton("Start NetMock Bridge").apply {
            preferredSize = Dimension(preferredSize.width, 36)
        }
        mainPanel.add(toggleButton, gbc)
        gbc.gridy++

        mainPanel.add(JSeparator(), gbc)
        gbc.gridy++

        mainPanel.add(JBLabel("Artificial Latency:"), gbc)
        gbc.gridy++

        val latencyDisplayOptions = arrayOf(
            "No Delay (0s)",
            "1 Second (1000ms)",
            "3 Seconds (3000ms)",
            "5 Seconds (5000ms)",
            "7 Seconds (7000ms)",
            "10 Seconds (10000ms)",
            "15 Seconds (15000ms)",
            "20 Seconds (20000ms)"
        )

        val latencyValueMapping = longArrayOf(0L, 1000L, 3000L, 5000L, 7000L, 10000L, 15000L, 20000L)

        val latencyDropdown = JComboBox(latencyDisplayOptions)
        mainPanel.add(latencyDropdown, gbc)
        gbc.gridy++

        mainPanel.add(JSeparator(), gbc)
        gbc.gridy++

        // --- 3. Rules Strategy Picker Section ---
        mainPanel.add(JBLabel("Interception Target:"), gbc)
        gbc.gridy++

        val restRadio = JBRadioButton("REST APIs Only", true)
        val graphqlRadio = JBRadioButton("GraphQL Only")
        val globalRadio = JBRadioButton("Global (All Traffic)")

        ButtonGroup().apply {
            add(restRadio)
            add(graphqlRadio)
            add(globalRadio)
        }

        mainPanel.add(restRadio, gbc)
        gbc.gridy++
        mainPanel.add(graphqlRadio, gbc)
        gbc.gridy++
        mainPanel.add(globalRadio, gbc)
        gbc.gridy++

        mainPanel.add(JBLabel("Target Endpoint or Operation Name:"), gbc)
        gbc.gridy++

        val targetTextField = JTextField().apply {
            toolTipText = "e.g., /api/v1/users or GetUserProfile"
        }
        mainPanel.add(targetTextField, gbc)
        gbc.gridy++


        fun sendLiveConfigUpdate() {
            if (!isBridgeActive) return

            val mode = when {
                restRadio.isSelected -> "REST"
                graphqlRadio.isSelected -> "GRAPHQL"
                else -> "GLOBAL"
            }

            val selectedIndex = latencyDropdown.selectedIndex
            val rawMilliseconds = if (selectedIndex in latencyValueMapping.indices) {
                latencyValueMapping[selectedIndex]
            } else {
                0L
            }

            val jsonPayload = """
                {
                  "latencyMs": $rawMilliseconds,
                  "targetMode": "$mode",
                  "targetPattern": "${targetTextField.text.trim()}"
                }
            """.trimIndent()

            adbManager.sendConfigurationUpdate(jsonPayload)
        }

        toggleButton.addActionListener {
            val selectedDevice = deviceDropdown.selectedItem as? String
            if (selectedDevice != null && selectedDevice != "No Active Devices Detected") {
                if (!isBridgeActive) {
                    val success = adbManager.setupPortForwarding(selectedDevice)
                    if (success) {
                        isBridgeActive = true
                        toggleButton.text = "Stop Bridge & Disconnect"
                        sendLiveConfigUpdate()
                    }
                } else {
                    isBridgeActive = false
                    toggleButton.text = "Start NetMock Bridge"
                }
            }
        }

        latencyDropdown.addActionListener { sendLiveConfigUpdate() }

        val liveUpdateListener = java.awt.event.ActionListener { sendLiveConfigUpdate() }
        restRadio.addActionListener(liveUpdateListener)
        graphqlRadio.addActionListener(liveUpdateListener)
        globalRadio.addActionListener(liveUpdateListener)
        targetTextField.addActionListener { sendLiveConfigUpdate() }

        val spacer = JBPanel<JBPanel<*>>()
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        mainPanel.add(spacer, gbc)

        val contentFactory = ContentFactory.getInstance()
        val windowContent = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(windowContent)
    }
}
