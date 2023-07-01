#!/bin/bash

if [ ! -d "signing/play" ]; then
  mkdir -p signing/play
  echo "$SIGNING_KEYSTORE_PLAY" | base64 --decode >signing/play/signing.keystore
  echo "$SIGNING_PROPERTIES_PLAY" | base64 --decode >signing/play/signing.properties
fi

if [ ! -d "signing/github" ]; then
  mkdir -p signing/github
  echo "$SIGNING_KEYSTORE_GITHUB" | base64 --decode >signing/github/signing.keystore
  echo "$SIGNING_PROPERTIES_GITHUB" | base64 --decode >signing/github/signing.properties
fi

if [ ! -f "app/google-services.json" ]; then
  echo "$GOOGLE_SERVICES" | base64 --decode >app/google-services.json
fi
