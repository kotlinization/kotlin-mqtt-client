package com.github.kotlinizer.mqtt.jvm.sample

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.client.MqttClient
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val flowLogger = FlowLogger()
private val mqttClient = MqttClient(flowLogger)

fun main() = Window(title = "MQTT Client Sample") {
    val scope = rememberCoroutineScope()
    val connectionState = mqttClient.connectionStatusStateFlow.collectAsState()
    val logs = flowLogger.map { it.joinToString(System.lineSeparator()) }.collectAsState("")
    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    scope.launch {
                        mqttClient.connect(
                            MqttConnectionConfig(serverUri = "tcp://localhost:1883")
                        )
                    }
                }
            ) {
                Text("CONNECT")
            }
            Text(connectionState.value.name)
            Text(logs.value)
        }
    }
}