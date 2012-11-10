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
  "org.apache.thrift" % "libthrift" % "0.9.0" % "compile",
  "com.twitter" % "util-eval"   % "5.3.13",
  "ch.qos.logback" % "logback-core" % "1.0.7" % "compile",    
  "ch.qos.logback" % "logback-classic" % "1.0.7" % "runtime"
)

fork in run := true

javaOptions in run += "-Dlogback.configurationFile=config/logback.xml"

testOptions := Seq(Tests.Filter(s => Seq("Spec", "Unit").exists(s.endsWith(_))))
