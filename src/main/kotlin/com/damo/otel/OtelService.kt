package com.damo.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter
import org.apache.pulsar.client.api.TypedMessageBuilder

class OtelService {
    private val openTelemetry: OpenTelemetry = OtelSdkConfig.create()
    fun before(context: org.apache.pulsar.functions.api.Context): Span {

        val topic = context.inputTopics.iterator().next()
        val properties = context.currentRecord.properties

        val otelContext = openTelemetry.propagators.textMapPropagator.extract(Context.current(), properties, getter)
        val span = openTelemetry.getTracer("pulsar-function", "0.0.1")
            .spanBuilder("process $topic")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("topic", topic)
            .setParent(otelContext)
            .setSpanKind(SpanKind.CONSUMER)
            .startSpan()
        span.makeCurrent()
        return span
    }

    fun after(span: Span, messageBuilder: TypedMessageBuilder<String?>) {
        openTelemetry.propagators.textMapPropagator.inject(Context.current(), messageBuilder, setter)
        span.end()
    }

    companion object {

        // Getter used to extract context propagation information.
        val getter = object : TextMapGetter<Map<String, String>> {
            override fun get(carrier: Map<String, String>?, key: String): String? {
                return if (carrier?.containsKey(key) == true) {
                    val value = carrier[key]
                    value
                } else null
            }

            override fun keys(carrier: Map<String, String>?): Set<String>? {
                return carrier?.keys
            }
        }

        // Getter used to extract context propagation information.
        val setter =
            TextMapSetter<TypedMessageBuilder<String?>> { carrier, key, value -> carrier?.property(key, value) }
    }
}

