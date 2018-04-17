#!/usr/bin/env bash

dev=./`basename "$0"`

case $1 in
    up)
        ${dev} rot
        npm start --prefix ../eic-platform 1>dev.log 2>dev.err &
        echo -n $! > dev.pid
        ${dev} log
    ;;
    down)
        kill -9 $(cat dev.pid)
        rm dev.pid
    ;;
    restart)
        ${dev} down
        ${dev} up
    ;;
    log)
        tail -f dev.log dev.err
    ;;
    rot)
        mv dev.log dev.0.log
        mv dev.err dev.0.err
    ;;
esac
