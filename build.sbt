name := "Parallel Entity Resolution Framework"

version := "0.1"

scalaVersion := "2.13.1"

//http://premise.artifactoryonline.com/premise/public/com/esotericsoftware/minlog/minlog/1.2-slf4j-jdanbrown-0/minlog-1.2-slf4j-jdanbrown-0.pom
//resolvers += "ArtifactoryOnline for minlog" at "https://premise.artifactoryonline.com/premise/public/"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"

libraryDependencies += "com.github.alexandrnikitin" %% "bloom-filter" % "latest.release"

libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % "2.6.6"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.6"

libraryDependencies += "org.scify" % "jedai-core" % "3.1"

libraryDependencies += "org.scala-lang" % "scala-library" % "2.12.0"

