# hocs-casework-search-indexer

[![CodeQL](https://github.com/UKHomeOffice/hocs-casework-search-indexer/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/UKHomeOffice/hocs-casework-search-indexer/actions/workflows/codeql-analysis.yml)

This service will reindex all data in hocs-casework to the hocs-search `elasticsearch` index.

## Getting Started

### Prerequisites

* ```Java 17```
* ```Docker```
* ```LocalStack```

### Submodules

This project contains a 'ci' submodule with a docker-compose and infrastructure scripts in it.
Most modern IDEs will handle pulling this automatically for you, but if not

```console
$ git submodule update --init --recursive
```

## Docker Compose

This repository contains a [Docker Compose](https://docs.docker.com/compose/)
file.

### Start localstack (sqs, sns, s3, es)

From the project root run:

```console
$ docker-compose -f ./ci/docker-compose.yml -f ./ci/docker-compose.elastic.yml up -d postgres localstack
```

> With Docker using 4 GB of memory, this takes approximately 5 minutes to startup.

### Stop the services

From the project root run:

```console
$ docker-compose -f ./ci/docker-compose.yml stop
```

> This will retain data in the local database and other volumes.

## Running in an IDE

If you are using an IDE, such as IntelliJ, this service can be started by running
the ```HocsCaseworkSearchIndexerApplication``` main class.

You need to specify appropriate Spring profiles.
Paste `development,localstack` into the "Active profiles" box of your run configuration.

## Versioning

For versioning this project uses SemVer.

## Authors

This project is authored by the Home Office.

## License

This project is licensed under the MIT license. For details please see [License](LICENSE)
