#!/bin/bash

#mvn clean install

docker exec -it pulsar-broker /pulsar/bin/pulsar-admin clusters list

docker exec -it pulsar-broker /pulsar/bin/pulsar-admin topics create persistent://public/default/otel-ingest
docker exec -it pulsar-broker /pulsar/bin/pulsar-admin topics create persistent://public/default/otel-feed

docker exec -it pulsar-broker /pulsar/bin/pulsar-admin functions create \
  --jar /pulsar/conf/pulsar_otel_function-0.0.1-SNAPSHOT.jar \
  --classname com.damo.NewMessageFunction \
  --inputs persistent://public/default/otel-ingest \
  --output persistent://public/default/otel-feed \
  --name new-message-function