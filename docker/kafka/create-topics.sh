#!/usr/bin/env bash
# Creates the Ventas360 Kafka topics with settings tuned per data flow.
# Idempotent: existing topics are left untouched (--if-not-exists).
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka-1:29092,kafka-2:29092,kafka-3:29092}"
PARTITIONS=3
REPLICATION=3

create_topic() {
  local topic="$1"
  shift
  echo "Creating topic ${topic}"
  kafka-topics --bootstrap-server "${BOOTSTRAP}" --create --if-not-exists \
    --topic "${topic}" --partitions "${PARTITIONS}" --replication-factor "${REPLICATION}" \
    --config min.insync.replicas=2 "$@"
}

# Completed sales: an append-only event stream for analytics and inventory.
# Kept 7 days so consumers can replay a week of activity.
create_topic ventas360.sales.completed \
  --config cleanup.policy=delete \
  --config retention.ms=604800000

# Product changes: compacted, so the latest state per productId is always kept
# and a new consumer can rebuild the full catalog from the beginning.
create_topic ventas360.product.events \
  --config cleanup.policy=compact \
  --config min.compaction.lag.ms=60000 \
  --config delete.retention.ms=86400000

# Dead-letter topics: same partition count as their source topic, because the
# dead-letter publisher keeps the original partition number. Kept 14 days for
# inspection and manual replay.
create_topic ventas360.sales.completed.DLT \
  --config cleanup.policy=delete \
  --config retention.ms=1209600000

create_topic ventas360.product.events.DLT \
  --config cleanup.policy=delete \
  --config retention.ms=1209600000

kafka-topics --bootstrap-server "${BOOTSTRAP}" --describe --topics-with-overrides
echo "Kafka topics ready"
