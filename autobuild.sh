#!/usr/bin/env bash

for dirname in build clib
do
if [ -d $dirname ]; then
    rm -rf $dirname
    echo "$dirname is removed"
fi
done

mkdir build && cd build && cmake .. && cmake --build .
cd ..