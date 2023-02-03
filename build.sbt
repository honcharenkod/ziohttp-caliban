ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "ziohttp-caliban"
  )

resolvers ++= Seq(
  "Quill Generic" at "https://repo1.maven.org"
)


val zioHttp = "2.0.2"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.6",
  "io.d11" %% "zhttp" % "2.0.0-RC7",
  "io.d11" %% "zhttp-test" % "2.0.0-RC7" % Test,
  "com.github.ghostdogpr" %% "caliban" % zioHttp,
  "com.github.ghostdogpr" %% "caliban-zio-http" % zioHttp,
  "io.getquill" %% "quill-jdbc-zio" % "4.6.0",
  "org.postgresql" % "postgresql" % "42.5.3",
  "dev.zio" %% "zio-logging-slf4j" % "2.1.8",
  "org.slf4j" % "slf4j-api" % "2.0.5",
  "org.slf4j" % "slf4j-simple" % "2.0.5",
  "dev.zio" %% "zio-config" % "3.0.7",
  "dev.zio" %% "zio-config-typesafe" % "3.0.7",
  "dev.zio" %% "zio-config-magnolia" % "3.0.7",
  "com.github.jwt-scala" %% "jwt-core" % "9.1.2",
  "dev.zio" %% "zio-json" % "0.4.2"
)