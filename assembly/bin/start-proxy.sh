#!/usr/bin/env bash

BIN=`dirname "$0"`

CLASSPATH='../conf'

for f in ../lib/*.jar; do
  CLASSPATH=${CLASSPATH}:$f;
done

java -server -classpath ${CLASSPATH} org.ineto.niosocks.SocksLauncher `cd $BIN; pwd` $1

