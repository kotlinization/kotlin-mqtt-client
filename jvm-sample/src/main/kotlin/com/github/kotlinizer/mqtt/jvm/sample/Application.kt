@file:Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")

package com.github.kotlinizer.mqtt.jvm.sample

import com.github.kotlinizer.mqtt.MqttConnectionConfig
import com.github.kotlinizer.mqtt.MqttMessage
import com.github.kotlinizer.mqtt.client.MqttClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


private val clientMap = mutableMapOf<Int, MqttClient>()

private var selectedClient: MqttClient? = null

private val commandDelegates = mapOf(
    "select" to ::selectCommand,
    "connect" to ::connectCommand,
    "pub" to ::publishCommand,
    "sub" to ::subscribeCommand
)

suspend fun mainTest() {
    println("This is a Mqtt client sample project.\nPlease enter command:")
    while (true) {
        val command = readLine() ?: return
        val commandArgs = command.split(" ")
        val keyWord = commandArgs.firstOrNull() ?: return
        val delegate = commandDelegates[keyWord]
        if (delegate == null) {
            println("Unknown command. Try again.")
        } else {
            delegate(commandArgs - keyWord)
        }
    }
}

suspend fun selectCommand(commandArgs: List<String>) {
    try {
        val clientIndex = commandArgs.first().toInt()
        selectedClient = clientMap.getOrPut(clientIndex) { MqttClient(SampleLogger()) }
        println("Selected client with index: $clientIndex")
    } catch (t: Throwable) {
        println("Error while processing *select* command. ${t.message}")
    }
}

suspend fun connectCommand(commandArgs: List<String>) {
    try {
        val client = selectedClient ?: throw IllegalStateException("You must select client first.")
        client.connect(
            MqttConnectionConfig(
                serverUri = "tcp://localhost:1883",
                clientId = clientMap.entries.first { it.value == client }.key.toString()
            )
        )
        println("Client connection status: ${client.connectionStatusStateFlow.value}")
    } catch (t: Throwable) {
        println("Error while processing *connect* command. ${t.message}")
    }
}

suspend fun subscribeCommand(commandArgs: List<String>) {
    try {
        val client = selectedClient ?: throw IllegalStateException("You must select client first.")
        val topic = commandArgs.firstOrNull() ?: throw IllegalArgumentException("Topic is not provided.")
        val subscribeFlow = client.subscribe(topic)
        println("Client subscribed to $topic")
        GlobalScope.launch {
            subscribeFlow.collect { message ->
                println("Received message on: ${message.topic}. Message: ${message.content}")
            }
        }
    } catch (t: Throwable) {
        println("Error while processing *sub* command. ${t.message}")
    }
}

suspend fun publishCommand(commandArgs: List<String>) {
    try {
        val client = selectedClient ?: throw IllegalStateException("You must select client first.")
        val topic = commandArgs.firstOrNull() ?: throw IllegalArgumentException("Topic is not provided.")
        val content = commandArgs.getOrNull(1) ?: throw IllegalArgumentException("Content is not provided.")
        client.publishMessage(MqttMessage(topic, content))
        println("Published message successfully.")
    } catch (t: Throwable) {
        println("Error while processing *pub* command. ${t.message}")
    }
}