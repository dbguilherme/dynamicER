#!/bin/bash

echo "Starting execution of DynamicEr with new filter process"

datasets=("10K" "amazonGp" "dblpAcm" "cddb" "dblpScholar" "movies")
#datasets=("dblpAcm")

rm out.csv
for i in ${datasets[@]}
do
  sbt "runMain SequentialDirtyMainPrefix -d1 $i  -gt $i  -bc 0.05  -fi 0.05 -o out.csv -su 0 -th 50"
#  sbt "runMain SequentialDirtyMainPrefix -d1 $i  -gt $i  -bc 0.05  -fi 0.05 -o out.csv -su 1 -th 50"
#  sbt "runMain SequentialDirtyMainPrefix -d1 $i  -gt $i  -bc 0.05  -fi 0.05 -o out.csv -su 2 -th 50"
#  sbt "runMain SequentialDirtyMainPrefix -d1 $i  -gt $i  -bc 0.05  -fi 0.05 -o out.csv -su 3 -th 50"
#  exit
done
