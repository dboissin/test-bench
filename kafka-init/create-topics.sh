#!/bin/bash
# Wait for Kafka to be ready
cub kafka-ready -b kafka:29092 1 20

# Create topics
kafka-topics --create --if-not-exists --zookeeper zookeeper:2181 --partitions 1 --replication-factor 1 --topic petapp-events

echo -e 'Successfully created the following topics:'
kafka-topics --zookeeper zookeeper:2181 --list
