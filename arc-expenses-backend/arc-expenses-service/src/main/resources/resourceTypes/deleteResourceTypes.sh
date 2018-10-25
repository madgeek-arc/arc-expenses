#!/bin/bash

for i in   user organization project request institute approval payment; do
	curl -X DELETE -k  https://$1/arc-expenses-service/resourceType/$i
done
