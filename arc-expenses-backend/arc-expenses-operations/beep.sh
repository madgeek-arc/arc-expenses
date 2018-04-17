#!/usr/bin/env bash

installed () {
    command -v "$1" >/dev/null 2>&1
}

if installed paplay ; then
    paplay --volume 32768 /usr/share/sounds/freedesktop/stereo/complete.oga
fi

if installed notify-send ; then
    notify-send "$1"
fi
