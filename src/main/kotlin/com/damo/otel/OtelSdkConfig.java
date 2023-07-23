package com.damo.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import java.util.UUID;

public class OtelSdkConfig {

  public static OpenTelemetry create() {
    // Configure OpenTelemetry as early as possible
    final OpenTelemetry openTelemetry = openTelemetrySdk();
    return openTelemetry;

    // Register runtime metrics instrumentation
    // https://github.com/aws-observability/aws-otel-community/blob/master/sample-apps/java-sample-app/manual/src/main/java/software/amazon/adot/sampleapp/ManualApp.java
//    GarbageCollector.registerObservers(openTelemetrySdk);
  }

  private static OpenTelemetry openTelemetrySdk() {
    // Configure resource
    Resource resource = Resource.getDefault().toBuilder()
        .put(ResourceAttributes.SERVICE_NAME, "sdk-nr-config")
        .put(ResourceAttributes.SERVICE_INSTANCE_ID, UUID.randomUUID().toString())
        .build();

    // Configure tracer provider
    SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder()
        .setResource(resource)
        // New Relic's max attribute length is 4095 characters
        .setSpanLimits(
            SpanLimits.getDefault().toBuilder().setMaxAttributeValueLength(4095).build())
        .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
        // Add batch processor with otlp span exporter
        .addSpanProcessor(
            BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint("http://otelcollector.com")
                        .setCompression("gzip")
                        .setRetryPolicy(RetryPolicy.getDefault())
                        .build())
                .build());

    SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
        .addLogRecordProcessor(BatchLogRecordProcessor.builder(OtlpGrpcLogRecordExporter.builder().build()).build())
        .setResource(resource)
        .build();

    // Configure meter provider
    SdkMeterProviderBuilder sdkMeterProviderBuilder = SdkMeterProvider.builder()
        .setResource(resource)
        // Add periodic metric reader with otlp metric exporter
        .registerMetricReader(
            PeriodicMetricReader.builder(
                    OtlpGrpcMetricExporter.builder()
                        .setEndpoint("http://otelcollector.com")
                        .setCompression("gzip")
                        // IMPORTANT: New Relic requires metrics to be delta temporality
                        .setAggregationTemporalitySelector(
                            AggregationTemporalitySelector.deltaPreferred())
                        // Use exponential histogram aggregation for histogram instruments to
                        // produce better data and compression
                        .setDefaultAggregationSelector(
                            DefaultAggregationSelector.getDefault()
                                .with(
                                    InstrumentType.HISTOGRAM,
                                    Aggregation.base2ExponentialBucketHistogram()))
                        .setRetryPolicy(RetryPolicy.getDefault())
                        .build())
                .build());

    // Bring it all together
    return OpenTelemetrySdk.builder()
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .setTracerProvider(sdkTracerProviderBuilder.build())
        .setMeterProvider(sdkMeterProviderBuilder.build())
        .setLoggerProvider(sdkLoggerProvider)
        .buildAndRegisterGlobal();

  }
}