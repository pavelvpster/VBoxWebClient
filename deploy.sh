#!/bin/bash

if [ ! -d "build" ]; then
    exit 1
fi

DEST=/opt/VBoxWebClient

if [ -d "${DEST}" ]; then
    rm -rf "${DEST}"
fi

mkdir "${DEST}"

cp -a build/libs/*.jar "${DEST}"

cp -a 3rd-party/jax-ws/vboxjws.jar "${DEST}"
