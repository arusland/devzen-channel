#!/usr/bin/env bash

# mandatory environment variables: BOT_TOKEN, ALERT_CHAT_ID, API_ID, API_HASH, PHONE_NUMBER
# optional environment variables: DEBUG, when true send episode+mp3 to ALERT_CHAT_ID
# API_ID and API_HASH can be obtained from https://my.telegram.org/apps

docker run --rm \
    -v $(pwd)/logs:/app/logs \
    -v $(pwd)/state:/app/state \
    -e "BOT_TOKEN=$BOT_TOKEN" \
    -e "ALERT_CHAT_ID=$ALERT_CHAT_ID" \
    -e "API_ID=$API_ID" \
    -e "API_HASH=$API_HASH" \
    -e "PHONE_NUMBER=$PHONE_NUMBER" \
    -e "DEBUG=true" \
    --name devzentg \
    arusland/devzen-channel
