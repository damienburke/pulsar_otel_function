#!/bin/bash

mvn clean install -f ./otelagegnt/pom.xml

docker compose down --volumes
