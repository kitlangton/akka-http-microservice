enablePlugins(JavaAppPackaging)

name         := "akka-http-microservice"
organization := "com.theiterators"
version      := "1.0"
scalaVersion := "2.13.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

//Revolver.settings
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= {
  val akkaHttpV      = "10.2.4"
  val akkaV          = "2.6.14"
  val scalaTestV     = "3.2.8"
  val circeV         = "0.14.0"
  val akkaHttpCirceV = "1.36.0"

  Seq(
    "com.typesafe.akka"             %% "akka-actor"          % akkaV,
    "com.typesafe.akka"             %% "akka-stream"         % akkaV,
    "com.typesafe.akka"             %% "akka-http"           % akkaHttpV,
    "io.circe"                      %% "circe-core"          % circeV,
    "io.circe"                      %% "circe-generic"       % circeV,
    "de.heikoseeberger"             %% "akka-http-circe"     % akkaHttpCirceV,
    "com.typesafe.akka"             %% "akka-testkit"        % akkaV,
    "com.typesafe.akka"             %% "akka-http-testkit"   % akkaHttpV  % "test",
    "org.scalatest"                 %% "scalatest"           % scalaTestV % "test",
    "dev.zio"                       %% "zio"                 % "2.0.2",
    "dev.zio"                       %% "zio-json"            % "0.3.0",
    "dev.zio"                       %% "zio-config"          % "3.0.2",
    "dev.zio"                       %% "zio-config-magnolia" % "3.0.2",
    "dev.zio"                       %% "zio-config-typesafe" % "3.0.2",
    "dev.zio"                       %% "zio-http"            % "2.0.0-RC11+86-be769082+20221007-1131-SNAPSHOT",
    "com.softwaremill.sttp.client3" %% "zio"                 % "3.8.2"
  )
}
