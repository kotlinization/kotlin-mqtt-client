package com.github.kotlinizer.mqtt.internal.topic

import com.github.kotlinizer.mqtt.topic.TopicMatcher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TopicMatcherTest {

    @Test
    fun `received topic is same as subscribed topic`() {
        val sut = TopicMatcher()

        assertTrue { sut.matches("topic", "topic") }
    }

    @Test
    fun `received topic is different than subscribed topic`() {
        val sut = TopicMatcher()

        assertFalse { sut.matches("topic", "different") }
    }

    @Test
    fun `subscribed topic matches multi level wildcard`() {
        val subscribeTo = "sport/tennis/player1/#"
        val receivingTopics = listOf(
            "sport/tennis/player1", "sport/tennis/player1/ranking", "sport/tennis/player1/score/wimbledon"
        )
        val sut = TopicMatcher()

        receivingTopics.forEach {
            assertTrue("$subscribeTo doesn't match $it, but it must.") {
                sut.matches(subscribeTo, it)
            }
        }
    }

    @Test
    fun `wildcard matches every topics`() {
        val sut = TopicMatcher()
        val receivingTopics = listOf(
            "topic", "topic/v1", "any/thi/ng"
        )

        receivingTopics.forEach {
            assertTrue { sut.matches("#", it) }
        }
    }
}