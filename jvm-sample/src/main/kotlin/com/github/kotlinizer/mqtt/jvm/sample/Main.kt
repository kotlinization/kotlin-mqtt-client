package com.github.kotlinizer.mqtt.jvm.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.kotlinizer.mqtt.jvm.sample.screen.ConnectRow
import com.github.kotlinizer.mqtt.jvm.sample.screen.LogsColumn
import com.github.kotlinizer.mqtt.jvm.sample.screen.PublishRow
import com.github.kotlinizer.mqtt.jvm.sample.screen.SubscribeRow

fun main() = application {
    val presenter = remember { ClientPresenter() }
    val connectionState by presenter.connectionState.collectAsState()
    val logs by presenter.logs.collectAsState()

    Window(
        title = "MQTT Client Sample",
        onCloseRequest = this::exitApplication
    ) {

        val childModifier = Modifier.padding(8.dp)
        val childSpacing = Arrangement.spacedBy(16.dp)
        MaterialTheme {
            Column {
                Column(childModifier, childSpacing) {
                    ConnectRow(
                        connectionState = connectionState,
                        onConnect = presenter::connectToBroker,
                        onDisconnect = presenter::disconnectFromBroker
                    )
                }
                Divider()
                Column(childModifier, childSpacing) {
                    SubscribeRow(
                        connectionState = connectionState,
                        onSubscribe = presenter::subscribeToTopic
                    )
                }
                Divider()
                Column(childModifier, childSpacing) {
                    PublishRow(
                        connectionState = connectionState,
                        onPublish = presenter::publishMessage
                    )
                }
                Divider()
                Column(childModifier, childSpacing) {
                    LogsColumn(
                        logs = logs,
                        onClearLogs = presenter::clearLogs
                    )
                }
            }
        }
    }
}