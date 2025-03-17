#!/bin/bash

mkdir output
cp app/build/outputs/apk/debug/AospXpert-signed.apk MagiskModBase/system/priv-app/AospXpert/AospXpert.apk
cd MagiskModBase

FILENAME="AospXpert.zip"
zip -r ../output/$FILENAME *;