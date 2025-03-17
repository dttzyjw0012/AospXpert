#!/bin/bash

mkdir -p output
cp app/build/outputs/apk/release/AospXpert.apk MagiskModBase/system/priv-app/AospXpert/AospXpert.apk
cd MagiskModBase;
FILENAME="AospXpert.zip"

echo 1 > build.type
zip -r ../output/$FILENAME *;