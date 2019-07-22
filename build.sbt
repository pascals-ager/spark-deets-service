val sparkMetaSettings = Seq(
  organization := "userDeetService",
  version := "0.1"
)

val sparkScalaSettings = Seq(
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-target:jvm-1.8"),
  coverageEnabled := false,
  coverageExcludedPackages := "io.pascals.spark.models;io.pascals.spark.utils"
)

val sparkDependencies = Seq(
  "com.typesafe" % "config" % "1.3.1",
  "org.apache.spark" % "spark-core_2.11" % "2.3.3" % "provided",
  "org.apache.spark" % "spark-sql_2.11" % "2.3.3" % "provided",
  "org.scalactic" %% "scalactic" % "3.0.1" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)


lazy val assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case "defaults.conf" => MergeStrategy.concat
    case PathList("UnusedStubClass.class") => MergeStrategy.discard
    case x => val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  test in assembly := {}
)

lazy val sparkTestProcessor = (project in file("."))
  .settings(name := "spark-sql-processor")
  .settings(mainClass in Compile := Some("io.pascals.spark.UserDeetService"))
  .settings(mainClass in assembly := Some("io.pascals.spark.UserDeetService"))
  .settings(assemblyJarName in assembly := "user-deet-service.jar")
  .settings(sparkMetaSettings: _*)
  .settings(sparkScalaSettings: _*)
  .settings(libraryDependencies ++= sparkDependencies)
  .enablePlugins(AssemblyPlugin)
  .settings(assemblySettings: _*)