#!/bin/sh
MVN_HOME="/home/romano/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11"
exec "$MVN_HOME/bin/mvn" "$@"
