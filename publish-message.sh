#!/bin/bash

docker exec -it pulsar-broker /pulsar/bin/pulsar-client produce \
  persistent://public/default/otel-ingest \
  --num-produce 1 \
  --properties 'traceparent=00-0bf7651916cd43dd8448eb211c80319c-b9c7c989f97918e1-01' \
  --messages "Hello Traces"

docker logs pulsar-broker | grep "*io.opentelemetry.exporter.logging.LoggingSpanExporter"

