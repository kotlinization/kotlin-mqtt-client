package com.github.kotlinizer.mqtt.jvm.sample.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.kotlinizer.mqtt.MqttConnectionStatus

@Composable
fun ConnectRow(
    connectionState: MqttConnectionStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Text(
        text = "Connection",
        style = MaterialTheme.typography.caption
    )
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
                Button(onClick = onDisconnect) {
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
                    onClick = onConnect,
                    enabled = connectionState == MqttConnectionStatus.DISCONNECTED ||
                            connectionState == MqttConnectionStatus.ERROR
                ) {
                    Text("CONNECT")
                }
            }
        }
    }
}