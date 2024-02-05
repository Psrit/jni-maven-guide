#!/usr/bin/env bash

for dirname in build clib target
do
if [ -d $dirname ]; then
    rm -rf $dirname
    echo "$dirname is removed"
fi
done

mvn validate generate-resources
mkdir build && cd build && cmake .. && make
cd ..
mvn compile package