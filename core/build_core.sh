#!/usr/bin/env bash
# Нужны: Go (go.dev/dl) и Git. На Windows запускать в Git Bash.
set -euo pipefail
cd "$(dirname "$0")"
TAG="${FORK_TAG:-v1.10.1}"
[ -d build/vk-turn-proxy ] || git clone --depth 1 --branch "$TAG" https://github.com/Moroka8/vk-turn-proxy.git build/vk-turn-proxy
MOD=$(awk '/^module /{print $2}' build/vk-turn-proxy/go.mod | tr -d '\r')
mkdir -p build/vk-turn-proxy/cmd/encorecore
sed "s#__MOD__#$MOD#" main.go.tpl > build/vk-turn-proxy/cmd/encorecore/main.go
OUT="../android/app/src/main/jniLibs/arm64-v8a"; mkdir -p "$OUT"; OUT="$(cd "$OUT" && pwd)"
cd build/vk-turn-proxy
GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o "$OUT/libencorecore.so" ./cmd/encorecore
echo "OK: $OUT/libencorecore.so"
