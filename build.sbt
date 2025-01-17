ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "ziohttp-caliban"
  )

resolvers ++= Seq(
  "Quill Generic" at "https://repo1.maven.org"
)


val zioHttp = "2.2.1"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.13",
  "dev.zio" %% "zio-concurrent" % "2.0.13",
  "dev.zio" %% "zio-http" % "3.0.0-RC1",
  "com.github.ghostdogpr" %% "caliban" % zioHttp,
  "com.github.ghostdogpr" %% "caliban-zio-http" % zioHttp,
  "io.getquill" %% "quill-jdbc-zio" % "4.6.0",
  "org.postgresql" % "postgresql" % "42.5.4",
  "dev.zio" %% "zio-logging-slf4j" % "2.1.12",
  "org.slf4j" % "slf4j-api" % "2.0.5",
  "org.slf4j" % "slf4j-simple" % "2.0.5",
  "dev.zio" %% "zio-config" % "3.0.7",
  "dev.zio" %% "zio-config-typesafe" % "3.0.7",
  "dev.zio" %% "zio-config-magnolia" % "3.0.7",
  "com.github.jwt-scala" %% "jwt-core" % "9.2.0",
  "dev.zio" %% "zio-json" % "0.5.0",
  "io.github.nremond" %% "pbkdf2-scala" % "0.7.0",
  "dev.zio" %% "zio-logging" % "2.1.13",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.5.1"
)