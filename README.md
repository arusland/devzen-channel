# DevZen Podcast Episode Publisher

Command line program which publish new episode of [DevZen podcast](https://devzen.ru) (in Russian) into [@devzen](https://t.me/devzen) Telegram channel

## Build

```bash
mvn clean package -DskipTests
```

## Configuration
 * API_ID and API_HASH can be obtained from https://my.telegram.org/apps
 * BOT_TOKEN can be obtained from Telegram bot @BotFather

## TODO
⚠️ This program uses [kotlogram](https://github.com/badoualy/kotlogram) library which is not maintained anymore and uses Layer 66 of 
[Telegram API](https://core.telegram.org/api), which cannot be used for new authentication. If you have time, please, fix it 🙃  

## Run

```bash
mkdir -p ./state/files
# put config.properties to ./state/config.properties 
# or set environment variables: BOT_TOKEN, ALERT_CHAT_ID, API_ID, API_HASH, PHONE_NUMBER
java -jar target/devzentg-jar-with-dependencies.jar
```

## Run with Docker

```bash
# put config.properties to ./state/config.properties 
# or set environment variables: BOT_TOKEN, ALERT_CHAT_ID, API_ID, API_HASH, PHONE_NUMBER
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
```

## Links
* Podcast website: [https://devzen.ru](https://devzen.ru)
* Telegram channel with published episodes: [https://t.me/devzen](https://t.me/devzen)
* Telegram channel of the podcast: [https://t.me/devzen_live](https://t.me/devzen_live)
