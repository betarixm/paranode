# Paranode

[![CSED434@POSTECH](https://img.shields.io/badge/CSED434-POSTECH-c80150)](https://www.postech.ac.kr/eng)
[![CSED434@POSTECH](https://img.shields.io/badge/Fall-2023-775E64)](https://www.postech.ac.kr/eng)

**Paranode** is a project developed as part of POSTECH's CSED434 curriculum, dedicated to the implementation of distributed and parallel sorting for key-value records using the Scala programming language. Its primary objective is the development of a robust sorting system tailored for the distribution of extensive datasets, surpassing the limitations of available RAM and storage capacity. Leveraging Scala, Paranode orchestrates parallel data processing across multiple machines, preserving record integrity through key-based sorting.

## Getting Started

### Development Environment

#### Install pre-commit hooks

1. Install `pre-commit` package by [instructions](https://pre-commit.com/#installation).
1. Run `pre-commit install`.

Note that `pre-commit install` should be re-run if [`.pre-commit-config.yaml`](.pre-commit-config.yaml) changed.
