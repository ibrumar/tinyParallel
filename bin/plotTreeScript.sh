#!/bin/bash

if [ $# -ne 1 ]; then

  echo "Enter relative path to the .asl source file"
  exit

fi

./Asl -dot -ast file.dot -noexec $1
dot -Tpdf file.dot -o file.pdf
okular file.pdf
