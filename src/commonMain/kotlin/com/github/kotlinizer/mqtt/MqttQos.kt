package com.github.kotlinizer.mqtt

enum class MqttQos {
    AT_MOST_ONCE,

    //TODO semi implemented
    AT_LEAST_ONCE,

    //TODO Not fully implemented
    EXACTLY_ONCE
}