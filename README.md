# Apache Sentry PoC

## Prerequisite: Build and install Apache Sentry

Apache Sentry 2.2.0+ is needed. This version hasn't been released to Maven repositories, so you need to build and install it from sources.

```shell
$ git clone https://github.com/apache/sentry.git
$ mvn clean install -DskipTests=true
```

## Build the server and the client

Clone this repository and build the server and the client:

```shell
$ git clone https://github.com/ggolawski/sentry-poc.git
$ mvn clean package
```

## Run the PoC

Run the server:

```shell
$ ./server.sh
```

Run the client:

```shell
$ ./client <username>
```