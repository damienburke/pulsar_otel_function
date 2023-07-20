# pulsar-otel-function

This project illustrates an issue I have seen with OTel context propagation with Pulsar Functions.
A Pulsar function is essentially composed of a consumer and producer, and OTel does correctly create
a `receive` and `send` Span for the function. The trace ID though, is somehow getting dropped/stomped
within the function as the `receive` Span DOES have the OTel context set, but the `send` does not.

As the Pulsar function just uses a pulsar client under the hood, this is unexpected, as pulsar client works
well re OTel context propagation across "regular" producer/consumer apps.

## Requirements

* Docker
* Maven
* Java

## Workflow

*
    1. `publish-message.sh`      PUBLISHES_TO    `persistent://public/default/otel-ingest`
*
    2. `NewMessageFunction`      CONSUMES_FROM   `persistent://public/default/otel-ingest`
*
    3. `NewMessageFunction`      PUBLISHES_TO    `persistent://public/default/otel-feed`

## Steps to recreate

### 1. Startup Pulsar Broker & Function

```shell 
mvn clean install
docker compose down --volumes
docker compose up &
sleep 10
./create-pulsar-components.sh
```

### 2. Publish message

```shell 
./publish-message.sh
```

### 3. View trace IDs
The hard-coded initial messages traceId of `0bf7651916cd43dd8448eb211c80319c` DOES get propagated/extracted by the
function and we see something like this for the receive Span:

`public/default/new-message-function-0] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'persistent://public/default/otel-ingest receive' : 0bf7651916cd43dd8448eb211c80319c 82d22133714b780b CONSUMER [tracer: io.opentelemetry.pulsar-2.8:1.28.0-alpha]`

The send span though does not have this trace ID set, e.g.

`persistent://public/default/otel-feed publish' : 8e3836f20f561a5d8e7baf8afa2dfbd6 369076304906928a PRODUCER [tracer: io.opentelemetry.pulsar-2.8:1.28.0-alpha] AttributesMap{data={thread.name=public/default/new-message-function-0, messaging.destination.name=persistent://public/default/otel-feed`

In summary, while the pulsar function seems to correctly set the OTel context with the Pulsar Function, the Pulsar
Function send Span is not extracting this OTel context, and this can be seen as the send Span has a different trace Id.