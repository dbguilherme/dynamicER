#!/bin/bash

echo "Starting execution of DynamicEr with new filter process"

datasets=("amazonGp" "dblpAcm" "cddb" "dblpScholar" "movies")
# "cddb" "movies")

#rm out.csv
for i in ${datasets[@]}
do
  sbt "runMain SequentialDirtyMainPrefix -d1 $i  -gt $i  -bc 0.05  -fi 0.05 -o out.csv"
done
