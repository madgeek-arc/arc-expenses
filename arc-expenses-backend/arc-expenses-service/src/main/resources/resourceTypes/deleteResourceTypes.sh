#!/bin/bash

for i in   user organization project request institute approval payment; do
	curl -X DELETE --header 'Accept: application/json' http://$1/arc-expenses-service/resourceType/$i
done
