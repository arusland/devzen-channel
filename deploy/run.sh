#!/bin/bash

sdir="$(dirname $0)"

echo "Script dir=$sdir"

cd $sdir

echo "Killing all started devzen-telegram instances..."
pgrep -a -f devzen-telegram.jar | awk '{print $1;}' | while read -r a; do kill -9 $a; done

java -jar devzen-telegram.jar &
