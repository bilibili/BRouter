#!/bin/sh

./gradlew publishAll
./gradlew publishAll -PsystemProp.brouter.appcompat=true
