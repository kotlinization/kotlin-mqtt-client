@file:Suppress("FunctionName")

package com.github.kotlinizer.mqtt.jvm.sample.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.kotlinizer.mqtt.Logger
import com.github.kotlinizer.mqtt.jvm.sample.Log

@Composable
fun LogsColumn(logs: List<Log>, onClearLogs: () -> Unit) {
    var filteredLevel by remember { mutableStateOf(Logger.Level.TRACE.ordinal) }
    val filteredLogs = logs.filter {
        it.level.ordinal <= filteredLevel
    }

    Text(
        text = "Logs",
        style = MaterialTheme.typography.caption
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onClearLogs) {
            Text("Clear logs")
        }
        Text("Level")
        LevelSelector(filteredLevel) {
            filteredLevel = it
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (filteredLogs.isEmpty()) {
            MaterialTheme.typography.h6
            Text(
                "No logs",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.h6
            )
        }
        LazyColumn(
            reverseLayout = false,
            state = LazyListState()
        ) {
            items(filteredLogs.reversed()) { log ->
                val background = if (log.level.ordinal <= Logger.Level.ERROR.ordinal) {
                    Modifier.fillMaxWidth().background(Color.Red)
                } else {
                    Modifier.fillMaxWidth()
                }
                Row(background, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(log.timeMessage)
                    Text(log.level.name.first().toString())
                    Text(log.message)
                }
            }
        }
    }
}

@Composable
fun LevelSelector(level: Int, onChangeLevel: (Int) -> Unit) {
    val items = Logger.Level.values().map { it }
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(items[level].toString())
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { index, level ->
                DropdownMenuItem(
                    onClick = {
                        onChangeLevel(index)
                        expanded = false
                    }
                ) { Text(text = level.toString()) }
            }
        }
    }
}

