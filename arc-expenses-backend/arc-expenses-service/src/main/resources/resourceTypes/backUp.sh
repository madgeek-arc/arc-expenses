#!/usr/bin/env bash

curl -X GET -k --header -o /tmp/test 'Accept: application/octet-stream' 'https://aleka.athenarc.gr/arc-expenses-service/dump/?schema=true&version=true'
