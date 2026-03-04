#!/usr/bin/env bash
set -euo pipefail

APP_NAME="SpaceKeeper"
MAIN_MODULE="com.spacekeeper"
MAIN_CLASS="com.spacekeeper.App"
RUNTIME_DIR="build/runtime"
DIST_DIR="dist"
ICON_PNG="icons/app.png"


mvn -q -DskipTests package
rm -rf "$RUNTIME_DIR"
jlink \
  --add-modules java.base,java.logging,java.sql,java.xml,java.desktop,jdk.unsupported,javafx.controls,javafx.fxml,javafx.graphics \
  --strip-debug --no-header-files --no-man-pages \
  --compress=2 \
  --output "$RUNTIME_DIR"

mkdir -p "$DIST_DIR"
jpackage \
  --name "$APP_NAME" \
  --input target \
  --main-jar spacekeeperfx-0.1.0-SNAPSHOT.jar \
  --main-class "$MAIN_CLASS" \
  --runtime-image "$RUNTIME_DIR" \
  --icon "$ICON_PNG" \
  --dest "$DIST_DIR" \
  --app-version "0.1.0" \
  --vendor "SpaceKeeper" \
  --copyright "2025 SpaceKeeper" \
  --type app-image
