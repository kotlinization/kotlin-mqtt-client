package com.github.kotlinizer.mqtt.jvm.sample

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttConnectionStatus
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.client.MqttClient
import com.github.kotlinizer.mqtt.jvm.sample.screen.logsColumn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val flowLogger = FlowLogger()
private val mqttClient = MqttClient(flowLogger)

fun main() = Window(title = "MQTT Client Sample") {
    val scope = rememberCoroutineScope()
    val connectionState by mqttClient.connectionStatusStateFlow.collectAsState()
    val logs by flowLogger.collectAsState()
    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(8.dp), Arrangement.spacedBy(16.dp)) {
            Column { connectRow(scope, connectionState) }
            Column { subscribeRow(scope, connectionState) }
            Column { publishRow(scope, connectionState) }
            Column { logsColumn(logs, flowLogger::clearLogs) }
        }
    }
}

@Composable
private fun connectRow(
    scope: CoroutineScope,
    connectionState: MqttConnectionStatus
) {
    divider("Connection")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(connectionState.name)
        when (connectionState) {
            MqttConnectionStatus.CONNECTING -> {
                Button({}, enabled = false) {
                    Text("CONNECTING")
                }
            }
            MqttConnectionStatus.ESTABLISHING -> {
                Button({}, enabled = false) {
                    Text("ESTABLISHING")
                }
            }
            MqttConnectionStatus.CONNECTED -> {
                Button(
                    onClick = {
                        scope.launch {
                            mqttClient.disconnect()
                        }
                    }
                ) {
                    Text("DISCONNECT")
                }
            }
            MqttConnectionStatus.DISCONNECTING -> {
                Button({}, enabled = false) {
                    Text("DISCONNECTING")
                }
            }
            MqttConnectionStatus.DISCONNECTED, MqttConnectionStatus.ERROR -> {
                Button(
                    onClick = {
                        scope.launch {
                            mqttClient.connect(
                                MqttConnectionConfig(serverUri = "tcp://localhost:1883")
                            )
                        }
                    },
                    enabled = connectionState == MqttConnectionStatus.DISCONNECTED ||
                            connectionState == MqttConnectionStatus.ERROR
                ) {
                    Text("CONNECT")
                }
            }
        }
    }
}

@Composable
fun subscribeRow(
    scope: CoroutineScope,
    connectionState: MqttConnectionStatus
) {
    var subscribeTopic by remember { mutableStateOf("") }
    divider("Subscribe")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = subscribeTopic,
            onValueChange = { subscribeTopic = it },
            label = { Text("Topic name") },
            enabled = connectionState == MqttConnectionStatus.CONNECTED
        )
        Button(
            onClick = {
                scope.launch {
                    mqttClient.subscribe(subscribeTopic)
                }
            },
            enabled = connectionState == MqttConnectionStatus.CONNECTED
        ) {
            Text("SUBSCRIBE")
        }
    }
}

@Composable
fun publishRow(
    scope: CoroutineScope,
    connectionState: MqttConnectionStatus
) {
    var publishTopic by remember { mutableStateOf("") }
    var publishContent by remember { mutableStateOf("") }
    divider("Publish")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = publishTopic,
            onValueChange = { publishTopic = it },
            label = { Text("Topic name") },
            enabled = connectionState == MqttConnectionStatus.CONNECTED
        )
        OutlinedTextField(
            value = publishContent,
            onValueChange = { publishContent = it },
            label = { Text("Message") },
            enabled = connectionState == MqttConnectionStatus.CONNECTED
        )
        Button(
            onClick = {
                scope.launch {
                    mqttClient.publishMessage(MqttMessage(publishTopic, publishContent))
                }
            },
            enabled = connectionState == MqttConnectionStatus.CONNECTED
        ) {
            Text("SEND")
        }
    }
}

@Composable
fun divider(name: String) {
    Text(name, style = MaterialTheme.typography.caption)
}