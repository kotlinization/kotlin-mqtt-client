package com.github.kotlinizer.mqtt

import io.moquette.broker.Server
import io.moquette.broker.config.MemoryConfig
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.Properties

class MqttBrokerRule : TestRule {

    private val broker by lazy { Server() }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                broker.startServer(MemoryConfig(Properties()))
                try {
                    base.evaluate()
                } finally {
                    broker.stopServer()
                }
            }
        }
    }
}