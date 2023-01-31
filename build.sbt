ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "ziohttp-caliban"
  )
val zioHttp = "2.0.2"
libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.6",
  "io.d11" %% "zhttp" % "2.0.0-RC11",
  "com.github.ghostdogpr" %% "caliban" % zioHttp,
  "com.github.ghostdogpr" %% "caliban-zio-http" % zioHttp
)