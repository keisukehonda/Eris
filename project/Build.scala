import sbt._
import Keys._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{ MultiJvm }

object Eris extends Build {
  lazy val root = Project(
    id = "Eris",
    base = file("."),
    settings = 
      Defaults.defaultSettings ++ multiJvmSettings ++ Seq(      
	  version := "0.9",
	  scalaVersion :="2.10.0",
	  resolvers ++= Resolvers.site,
	  libraryDependencies ++= Dependencies.libs,
	fork in run := true,
	javaOptions in run += "-Dlogback.configurationFile=config/logback.xml",
	testOptions := Seq(Tests.Filter(s => Seq("Spec", "Unit").exists(s.endsWith(_))))
      )
  ) configs(MultiJvm)
  
  lazy val multiJvmSettings = SbtMultiJvm.multiJvmSettings ++ Seq(
    // make sure that MultiJvm test are compiled by the default test compilation
    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
    // disable parallel tests
    parallelExecution in Test := false,
    // make sure that MultiJvm tests are executed by the default test target
    executeTests in Test <<=
      ((executeTests in Test), (executeTests in MultiJvm)) map {
        case ((_, testResults), (_, multiJvmResults))  =>
          val results = testResults ++ multiJvmResults
          (Tests.overall(results.values), results)
    }
  )
  
  object Dependencies {
    val libs = Seq(
      // ---- application dependencies ----
      "com.typesafe.akka" %% "akka-actor" % "2.2-M3",
      "com.typesafe.akka" %% "akka-remote" % "2.2-M3",
      "com.typesafe.akka" %% "akka-slf4j" % "2.1.1",
      "com.typesafe.slick" %% "slick" % "1.0.0",
      "postgresql" % "postgresql" % "9.1-901.jdbc4",      
      "com.twitter" % "util-eval_2.10" % "6.1.0",
      "ch.qos.logback" % "logback-core" % "1.0.7" % "compile",
      "ch.qos.logback" % "logback-classic" % "1.0.7" % "runtime",
      // ---- test dependencies ----
      "com.typesafe.akka" %% "akka-testkit" % "2.2-M3" % "test" ,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % "2.2-M3" % "test" ,
      "org.specs2" %% "specs2" % "1.14" % "test"
   )
  }

  object Resolvers {
    val site = Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "bigtoast-github" at "http://bigtoast.github.com/repo/",
      "twttrRepo" at "http://maven.twttr.com",
      "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "releases"  at "http://oss.sonatype.org/content/repositories/releases"
    )
  }

}
