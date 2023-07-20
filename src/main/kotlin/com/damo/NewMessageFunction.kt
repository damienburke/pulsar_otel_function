package com.damo

import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class NewMessageFunction : Function<ByteArray?, Void?> {
    override fun process(input: ByteArray?, context: Context): Void? {

        context.logger.info("function properties ${context.currentRecord.properties.toString()}")

        context.currentRecord.properties.forEach { (t, u) -> println("T:$t U:$u") }

        context.currentRecord.properties.forEach { (key, value) -> println("Header: $key = $value") }

        context.newOutputMessage(context.outputTopic, Schema.STRING)
            .key("key1")
            .value("value3")
            .property("foo", "bar")
            .send()

        return null
    }
}