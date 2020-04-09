#!/bin/sh

rm -rf tmp
java -cp sentry-poc-server/src/main/resources/:sentry-poc-server/target/sentry-poc-server-1.0-SNAPSHOT.jar:sentry-poc-server/target/libs/* org.ggolawski.sentry.poc.server.SentryPocServer
