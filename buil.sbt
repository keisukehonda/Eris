name := "Eris"

version := "0.9"

scalaVersion :="2.9.2"

retrieveManaged := true

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "bigtoast-github" at "http://bigtoast.github.com/repo/",
  "twttrRepo" at "http://maven.twttr.com",
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "http://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % "2.9.2" % "compile",
  "com.typesafe.akka" % "akka-actor" % "2.0.3" % "compile",
  "com.typesafe.akka" % "akka-remote" % "2.0.3" % "compile",  
  "com.typesafe.akka" % "akka-slf4j" % "2.0.3" % "compile",  
  "org.apache.thrift" % "libthrift" % "0.5.0" % "compile",
  "com.twitter" % "ostrich" % "4.0.1" % "compile",
  "ch.qos.logback" % "logback-core" % "1.0.1" % "compile",    
  "ch.qos.logback" % "logback-classic" % "1.0.1" % "runtime",
  "com.typesafe.akka" % "akka-testkit" % "2.0.2" % "test",
  "org.specs2" %% "specs2" % "1.11" % "test",
  "junit" % "junit" % "4.8.1" % "test",
  "org.scalatest" %% "scalatest" % "1.8" % "test"
)

fork in run := true

javaOptions in run += "-Dlogback.configurationFile=config/logback.xml"

testOptions := Seq(Tests.Filter(s => Seq("Spec", "Unit").exists(s.endsWith(_))))
