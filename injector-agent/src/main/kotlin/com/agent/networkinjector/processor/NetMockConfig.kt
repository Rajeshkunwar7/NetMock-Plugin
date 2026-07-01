package com.agent.networkinjector.processor

object NetMockConfig {

    @Volatile
    var latencyMs: Long = 0

    @Volatile
    var targetMode: String = "GLOBAL"

    @Volatile
    var targetPattern: String = ""

    fun reset() {
        latencyMs = 0
        targetMode = "GLOBAL"
        targetPattern = ""
    }

}
