#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: $0 <sourceFile[without .tpl]>"
  exit
fi
echo "Precompiling"
./Asl $1.tpl > $1.cpp
echo "Compiling"
g++ $1.cpp -O3 -o $1 -fopenmp 
