import sbt._
import Keys._


object Eris extends Build {
  lazy val root = Project(
    id = "Eris",
    base = file("."),
    settings = 
      Defaults.defaultSettings     
      ++ Seq(      
	  version := "0.9",
	  scalaVersion :="2.10.0",
	  resolvers ++= Seq(
	    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
	    "bigtoast-github" at "http://bigtoast.github.com/repo/",
	    "twttrRepo" at "http://maven.twttr.com",
	    "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
	    "releases"  at "http://oss.sonatype.org/content/repositories/releases"),
	  libraryDependencies ++= Seq(
	    "com.typesafe.akka" %% "akka-actor" % "2.1.0",
	    "com.typesafe.akka" %% "akka-remote" % "2.1.0",
	    "com.typesafe.akka" %% "akka-slf4j" % "2.1.0",
	    "com.typesafe.slick" %% "slick" % "1.0.0",
	    "postgresql" % "postgresql" % "9.1-901.jdbc4",
	    "org.apache.thrift" % "libthrift" % "0.9.0" % "compile",
	    "com.twitter" % "util-eval_2.10" % "6.1.0",
	    "ch.qos.logback" % "logback-core" % "1.0.7" % "compile",
	    "ch.qos.logback" % "logback-classic" % "1.0.7" % "runtime"),
	fork in run := true,
	javaOptions in run += "-Dlogback.configurationFile=config/logback.xml",
	testOptions := Seq(Tests.Filter(s => Seq("Spec", "Unit").exists(s.endsWith(_))))
	)
  )
}