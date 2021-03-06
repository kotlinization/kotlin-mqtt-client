package com.github.kotlinizer.mqtt.jvm.sample

import androidx.compose.desktop.Window
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttConnectionStatus
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.client.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val flowLogger = FlowLogger()
private val mqttClient = MqttClient(flowLogger)

fun main() = Window(title = "MQTT Client Sample") {
    val scope = rememberCoroutineScope()
    val connectionState = mqttClient.connectionStatusStateFlow.collectAsState()
    MaterialTheme {
        Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
            connectRow(scope, connectionState)
            subscribeRow(scope, connectionState)
            publishRow(scope, connectionState)
            logsColumn()
        }
    }
}

@Composable
private fun connectRow(
    scope: CoroutineScope,
    connectionState: State<MqttConnectionStatus>
) {
    divider("Connection")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(connectionState.value.name)
        Button(
            onClick = {
                scope.launch {
                    mqttClient.connect(
                        MqttConnectionConfig(serverUri = "tcp://localhost:1883")
                    )
                }
            },
            enabled = connectionState.value == MqttConnectionStatus.DISCONNECTED ||
                    connectionState.value == MqttConnectionStatus.ERROR
        ) {
            Text("CONNECT")
        }
        Button(
            onClick = {
                scope.launch {
                    mqttClient.disconnect()
                }
            },
            enabled = connectionState.value == MqttConnectionStatus.CONNECTED
        ) {
            Text("DISCONNECT")
        }
    }
}

@Composable
fun subscribeRow(
    scope: CoroutineScope,
    connectionState: State<MqttConnectionStatus>
) {
    val subscribeTopic = remember { mutableStateOf("") }
    divider("Subscribe")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = subscribeTopic.value,
            onValueChange = { subscribeTopic.value = it },
            label = { Text("Topic name") },
            enabled = connectionState.value == MqttConnectionStatus.CONNECTED
        )
        Button(
            onClick = {
                scope.launch {
                    mqttClient.subscribe(subscribeTopic.value)
                }
            },
            enabled = connectionState.value == MqttConnectionStatus.CONNECTED
        ) {
            Text("SUBSCRIBE")
        }
    }
}

@Composable
fun publishRow(
    scope: CoroutineScope,
    connectionState: State<MqttConnectionStatus>
) {
    val publishTopic = remember { mutableStateOf("") }
    val publishContent = remember { mutableStateOf("") }
    divider("Publish")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = publishTopic.value,
            onValueChange = { publishTopic.value = it },
            label = { Text("Topic name") },
            enabled = connectionState.value == MqttConnectionStatus.CONNECTED
        )
        OutlinedTextField(
            value = publishContent.value,
            onValueChange = { publishContent.value = it },
            label = { Text("Message") },
            enabled = connectionState.value == MqttConnectionStatus.CONNECTED
        )
        Button(
            onClick = {
                scope.launch {
                    mqttClient.publishMessage(
                        MqttMessage(
                            publishTopic.value,
                            publishContent.value
                        )
                    )
                }
            },
            enabled = connectionState.value == MqttConnectionStatus.CONNECTED
        ) {
            Text("SEND")
        }
    }
}

@Composable
fun logsColumn() {
    val logs = flowLogger.map { it.reversed() }.collectAsState(emptyList())
    divider("Logs")
    LazyColumn(
        reverseLayout = false,
        state = LazyListState()
    ) {
        items(logs.value) { log -> Text(log) }
    }
}

@Composable
fun divider(name: String) {
    Text(name, fontSize = 10.sp)
}