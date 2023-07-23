package com.damo

import com.damo.otel.OtelAwareFunction
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.functions.api.Context

open class NewMessageFunction : OtelAwareFunction<ByteArray?, Void?>() {
    override fun createMessageBuilder(input: ByteArray?, context: Context): TypedMessageBuilder<String?> {

        return context.newOutputMessage(context.outputTopic, Schema.STRING)
            .key("key1")
            .value("value3")
            .property("foo", "bar")
    }
}