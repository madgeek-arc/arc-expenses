#!/bin/bash

response=$(curl --write-out %{http_code} -L -o dump.zip http://marilyn.athenarc.gr:8010/arc-expenses-service/dump/?schema=true&version=true)
while $response!=200
do
    printf '.'
    sleep 5
done
unzip dump.zip
mv approval/ payment/ user/ organization/ project/ request/ institute/ backup
rsync -avzh root@marilyn.athenarc.gr:/var/lib/docker/volumes/arc_storeData/ backup