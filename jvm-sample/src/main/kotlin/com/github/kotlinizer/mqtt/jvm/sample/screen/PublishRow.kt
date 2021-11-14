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
import com.github.kotlinizer.mqtt.MqttMessage

@Composable
fun PublishRow(
    connectionState: MqttConnectionStatus,
    onPublish: (MqttMessage) -> Unit
) {
    var publishTopic by remember { mutableStateOf("") }
    var publishContent by remember { mutableStateOf("") }

    Text(
        text = "Publish",
        style = MaterialTheme.typography.caption
    )
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
                onPublish(MqttMessage(publishTopic, publishContent))
            },
            enabled = connectionState == MqttConnectionStatus.CONNECTED
        ) {
            Text("SEND")
        }
    }
}