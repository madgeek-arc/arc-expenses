#!/usr/bin/env bash

curl -L -o dump.zip http://$1:8010/arc-expenses-service/dump/?schema=true&version=true
rsync -avzh /var/lib/docker/volumes/arc_storeData/ /data/backup