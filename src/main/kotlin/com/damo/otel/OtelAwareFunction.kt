package com.damo.otel

import org.apache.pulsar.client.api.TypedMessageBuilder
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

abstract class OtelAwareFunction<T, U> : Function<ByteArray?, Void?> {

    private var otelService: OtelService? = null

    private fun getOtelService(): OtelService {
        if (this.otelService == null) {
            this.otelService = OtelService()
        }
        return this.otelService!!
    }

    override fun process(input: ByteArray?, context: Context): Void? {

        val span = getOtelService().before(context)

        val messageBuilder = createMessageBuilder(input, context)

        getOtelService().after(span, messageBuilder)

        messageBuilder.send()

        return null
    }

    abstract fun createMessageBuilder(input: ByteArray?, context: Context): TypedMessageBuilder<String?>
}

