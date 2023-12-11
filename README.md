# Paranode

[![CSED434@POSTECH](https://img.shields.io/badge/CSED434-POSTECH-c80150)](https://www.postech.ac.kr/eng)
[![CSED434@POSTECH](https://img.shields.io/badge/Fall-2023-775E64)](https://www.postech.ac.kr/eng)

**Paranode** is a project developed as part of POSTECH's CSED434 curriculum, dedicated to the implementation of **distributed and parallel sorting system** for key-value records using the Scala programming language. Its primary objective is the development of a robust sorting system tailored for the distribution of extensive datasets, surpassing the limitations of available RAM and storage capacity. Leveraging Scala, Paranode orchestrates parallel data processing across multiple machines, preserving record integrity through key-based sorting.

## Usage

### Build

To build the project, follow these instructions:

1. Clone the repository.
1. Run `sbt clean`.
1. Run `sbt assembly`.

### Run

To launch a master node and worker nodes, follow these steps:

1. Build the project using the instructions above.
1. Ensure that `master` and `worker` executables are present in the [`build`](build) directory.
1. Run [`master`](build/master) with parameters.
    - For example, `./master <NUMBER_OF_WORKERS>`.
1. Run [`worker`](build/worker) with parameters.
    - For example, `./worker <MASTER_HOST>:<MASTER_PORT> -I <INPUT_DIRECTORY> <INPUT_DIRECTORY> <...> -O <OUTPUT_DIRECTORY>`.

Note that built executables consist of shell scripts and JAR files, and thus require systems that support running shell scripts and have Java installed.

## Development

### Environment

- Scala 2.13.12
- SBT 1.9.6
- Java 20

### Test

To run unit tests, follow the instructions below:

1. Clone the repository.
1. Run `sbt test`.

To run e2e tests, follow the instructions below:

1. Clone the repository.
1. Run `sbt e2e/test`.

### Pre-commit hooks

1. Install `pre-commit` package by [instructions](https://pre-commit.com/#installation).
1. Run `pre-commit install`.

Note that `pre-commit install` should be re-run if [`.pre-commit-config.yaml`](.pre-commit-config.yaml) changed.

## Disclaimer

This project is developed as part of the CSED434 course at POSTECH, and is not intended for production or commercial use. Any usage is at your own risk, and the contributors are not responsible for any potential issues.
