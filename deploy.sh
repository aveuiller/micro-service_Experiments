#!/bin/bash

sbt docker:publishLocal

cd "deployment"; docker-compose up -d; cd -
