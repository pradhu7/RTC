#!/bin/bash
set -euo pipefail

OUTPUT_FORMAT="markdown"
PROTO_BASE_DIR="${1:-"/protos/src/main/protobuf"}"
PROTO_BASE_DIR="$(readlink -f "$PROTO_BASE_DIR")"
FLATTEN_PROTO_INCLUDE=true
PROTO_INCLUDE_DIRS=()
PROTO_DIRS=( $(find "$PROTO_BASE_DIR" -name '*.proto' | xargs -n1 dirname | sort -u | xargs -n1 readlink -f) )

if [[ "$FLATTEN_PROTO_INCLUDE" == "true" ]];then
  for DIR in "${PROTO_DIRS[@]}";do
    PROTO_INCLUDE_DIRS+=( "-I$DIR" )
  done
else
  PROTO_INCLUDE_DIRS+=( "-I$PROTO_BASE_DIR" )
fi

for PROTO_DIR in "${PROTO_DIRS[@]}";do
  PROTO_FILES=( $(find $PROTO_DIR -name '*.proto') )
  OUTPUT_DIR="${PROTO_DIR#$PROTO_BASE_DIR}"
  OUTPUT_DIR="${OUTPUT_DIR#"/"}"
  for PROTO_FILE in "${PROTO_FILES[@]}";do
    OUTPUT_FILE="$OUTPUT_DIR/$(basename $PROTO_FILE .proto).md"
    protoc "${PROTO_INCLUDE_DIRS[@]}" --doc_out=/out --doc_opt="$OUTPUT_FORMAT,out.md" "$PROTO_FILE"
    mkdir -p "/out/$OUTPUT_DIR"
    mv /out/out.md "/out/$OUTPUT_FILE"
  done
done
