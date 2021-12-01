# End-to-end Task Based Parallelization for Entity Resolution on Dynamic Data
This is the source code for the framework proposed in the paper
 
 L. Gazzarri, and M. Herschel. "End-to-end Task Based Parallelization for Entity Resolution on Dynamic Data." ICDE 2021

#### Datasets
All the datasets have been downloaded from the [JedAI](https://github.com/scify/JedAIToolkit/) repository.
Datasets for 'cora', 'cddb', and 'amazon-google are in `data/dirtyErDatasets`. 
The dataset for 'movies' is in `data/cleanCleanErDatasets`.
The larger dataset 'dbpedia' can be downloaded from this [Mendeley](https://data.mendeley.com/datasets/4whpm32y47/7) repository, used to assess JedAI performance.
- dataset 'dbpedia': file newDBPedia.tar.xz in Mendeley's Real Clean-Clean ER data

To download additional datasets from JedAI you can run the `data/download.sh` script from inside the `data/` directory (svn required). 

#### Requirements
The framework is written in [Scala](https://www.scala-lang.org/) (version 2.13.1) and it requires SBT and OpenJDK to be installed and executed.
- [SBT](https://www.scala-sbt.org/1.x/docs/Setup.html)
- [OpenJDK 11](https://openjdk.java.net/projects/jdk/11/)

Library dependencies are listed in the SBT configuration file `build.sbt`.

#### Installation
To install and download library dependencies:
```
sbt publishLocal
```

#### Run
Clean-Clean ER. To run the sequential program for the 'movies' dataset (imdb-dbpedia) .
``` 
sbt "runMain SequentialCCMain -d1 imdb -d2 dbpedia -gt movies  -bc 0.05  -fi 0.05 -o movies.csv"
```

Dirty ER. To run the sequential program for the 'cddb' dataset (2M).
``` 
sbt "runMain SequentialDirtyMain -d1 cddb -gt cddb -bc 0.05  -fi 0.05 -o cdb.csv"
```

Parallel Clean-Clean ER. To run the parallel program (PP) for the 'dbpedia' dataset. 
``` 
sbt "runMain AkkaPipelineNoSplitCCMain -d1 DBPedia1 -d2 DBPedia2 -gt DBPedia  -bc 0.005 -fi 0.05 -nb 2 -nc 6 -nw 12 -o dbpedia.csv"
```

Parallel Clean-Clean ER. To run the parallel program (MPP) for the 'dbpedia' dataset. 
``` 
sbt "runMain AkkaPipelineMicroBatchOptimizedNoSplitCCMain -d1 DBPedia1 -d2 DBPedia2 -gt DBPedia  -bc 0.005 -fi 0.05 -nb 2 -nc 6 -nw 12 -o dbpedia.csv"
```

About the options:
- '-d1' specifies the first dataset. 
- '-d2' specifies the second dataset (for Clean-Clean ER).
- '-gt' specifies the groundtruth file.'
- '-bc' and '-fi' specify the parameters for block pruning and block ghosting. For the dataset 'dbpedia' set `-bc 0.005`. 
- '-nb' specifies the number of threads performing comparison generation
- '-nc' specifies the number of threads performing comparison cleaning
- '-nw' specifies the number of threads performing the pairwise comparison step

For the parallel solutions, the total number of threads is 5+nb+nc+nw.

For larger datasets consider to increase the heap size. For example for 'dbpedia':
```
export SBT_OPTS="-Xmx40G"
```

#### Contact
For any problem contact me at leonardo.gazzarri@ipvs.uni-stuttgart.de
