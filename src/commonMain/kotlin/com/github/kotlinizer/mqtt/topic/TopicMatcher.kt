package com.github.kotlinizer.mqtt.topic

internal class TopicMatcher {

    fun matches(subscribedTo: String, receivedOn: String): Boolean {
        return if (subscribedTo.contains('#')) {
            val subscribedPrefix = subscribedTo
                .removeSuffix("#")
                .removeSuffix("/")
            receivedOn.startsWith(subscribedPrefix)
        } else {
            subscribedTo == receivedOn
        }
    }
}