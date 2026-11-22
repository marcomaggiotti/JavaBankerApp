#!/usr/bin/env bash
# Scaffold a new Kafka Streams challenge: a TopologyContributor + a TopologyTestDriver test.
#
#   scripts/new-challenge.sh LargeTransactionAlert [input-topic] [output-topic]
#
set -euo pipefail

NAME="${1:?usage: $0 <PascalCaseName> [input-topic] [output-topic]}"
[[ "$NAME" =~ ^[A-Z][A-Za-z0-9]+$ ]] || { echo "Name must be PascalCase, e.g. LargeTransactionAlert" >&2; exit 1; }

PKG="$(echo "$NAME" | tr '[:upper:]' '[:lower:]')"
KEBAB="$(echo "$NAME" | sed -E 's/([a-z0-9])([A-Z])/\1-\2/g' | tr '[:upper:]' '[:lower:]')"
INPUT_TOPIC="${2:-bank.transactions.v1}"
OUTPUT_TOPIC="${3:-bank.${KEBAB}.v1}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MAIN_DIR="$ROOT/src/main/java/com/javabanker/challenges/$PKG"
TEST_DIR="$ROOT/src/test/java/com/javabanker/challenges/$PKG"

if [[ -e "$MAIN_DIR/${NAME}Topology.java" ]]; then
  echo "Challenge $NAME already exists at $MAIN_DIR" >&2
  exit 1
fi
mkdir -p "$MAIN_DIR" "$TEST_DIR"

render() {
  sed -e "s/__NAME__/$NAME/g" -e "s/__PKG__/$PKG/g" \
      -e "s/__INPUT_TOPIC__/$INPUT_TOPIC/g" -e "s/__OUTPUT_TOPIC__/$OUTPUT_TOPIC/g" "$1" > "$2"
}
render "$ROOT/templates/challenge/Topology.java.tmpl"     "$MAIN_DIR/${NAME}Topology.java"
render "$ROOT/templates/challenge/TopologyTest.java.tmpl" "$TEST_DIR/${NAME}TopologyTest.java"

cat <<MSG
Created:
  $MAIN_DIR/${NAME}Topology.java
  $TEST_DIR/${NAME}TopologyTest.java

Next:
  1. Write the acceptance criteria as tests in ${NAME}TopologyTest
  2. mvn test -Dtest=${NAME}TopologyTest
  3. Register the bean in KafkaStreamsConfig to run it in the app
MSG
