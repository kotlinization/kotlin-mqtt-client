package com.github.kotlinizer.mqtt.jvm.sample.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.github.kotlinizer.mqtt.MqttConnectionStatus

@Composable
fun SubscribeRow(
    connectionState: MqttConnectionStatus,
    onSubscribe: (String) -> Unit
) {
    var subscribeTopic by remember { mutableStateOf("") }

    Text(
        text = "Subscribe",
        style = MaterialTheme.typography.caption
    )
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
            onClick = { onSubscribe(subscribeTopic) },
            enabled = connectionState == MqttConnectionStatus.CONNECTED
        ) {
            Text("SUBSCRIBE")
        }
    }
}
