import sbt._
import sbt.Keys._

object Dependencies {
  object AkkaServerless {
    val ProtocolVersionMajor = 0
    val ProtocolVersionMinor = 7
    val FrameworkVersion = "0.7.0-beta.18"
  }

  // changing the Scala version of the Java SDK affects end users
  val ScalaVersion = "2.13.6"
  val ScalaVersionForCodegen = Seq("2.12.14")

  val ProtobufVersion = akka.grpc.gen.BuildInfo.googleProtobufVersion

  val AkkaVersion = "2.6.16"
  val AkkaHttpVersion = "10.2.6" // Note: should at least the Akka HTTP version required by Akka gRPC
  val ScalaTestVersion = "3.2.7"
  val JacksonDatabindVersion = "2.11.4" // Akka 2.6.16: 2.11.4, google-http-client-jackson2 1.34.0: 2.10.1
  val DockerBaseImageVersion = "adoptopenjdk/openjdk11:debianslim-jre"
  val LogbackVersion = "1.2.3"
  val LogbackContribVersion = "0.1.5"
  val TestContainersVersion = "1.15.3"
  val JUnitVersion = "4.13.2"
  val JUnitInterfaceVersion = "0.11"
  val JUnitJupiterVersion = "5.7.1"

  val CommonsIoVerison = "2.8.0"
  val MunitVersion = "0.7.20"
  val ScoptVersions = "4.0.0"

  val akkaslsProxyProtocol = "com.akkaserverless" % "akkaserverless-proxy-protocol" % AkkaServerless.FrameworkVersion
  val akkaslsSdkProtocol = "com.akkaserverless" % "akkaserverless-sdk-protocol" % AkkaServerless.FrameworkVersion
  val akkaslsTckProtocol = "com.akkaserverless" % "akkaserverless-tck-protocol" % AkkaServerless.FrameworkVersion

  val commonsIo = "commons-io" % "commons-io" % CommonsIoVerison
  val logback = "ch.qos.logback" % "logback-classic" % LogbackVersion
  val logbackContrib = "ch.qos.logback.contrib" % "logback-json-classic" % LogbackContribVersion

  val protobufJava = "com.google.protobuf" % "protobuf-java" % ProtobufVersion
  val protobufJavaUtil = "com.google.protobuf" % "protobuf-java-util" % ProtobufVersion

  val scopt = "com.github.scopt" %% "scopt" % ScoptVersions
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % JacksonDatabindVersion

  val testcontainers = "org.testcontainers" % "testcontainers" % TestContainersVersion
  val scalaTest = "org.scalatest" %% "scalatest" % ScalaTestVersion
  val munit = "org.scalameta" %% "munit" % MunitVersion
  val munitScalaCheck = "org.scalameta" %% "munit-scalacheck" % MunitVersion
  val testContainers = "org.testcontainers" % "testcontainers" % TestContainersVersion
  val junit4 = "junit" % "junit" % JUnitVersion
  val junit5 = "org.junit.jupiter" % "junit-jupiter" % JUnitJupiterVersion

  private val deps = libraryDependencies

  val sdk = deps ++= Seq(
        akkaDependency("akka-stream"),
        akkaDependency("akka-slf4j"),
        akkaDependency("akka-discovery"),
        akkaHttpDependency("akka-http"),
        akkaHttpDependency("akka-http-core"),
        akkaHttpDependency("akka-http-spray-json"),
        akkaHttpDependency("akka-http2-support"),
        akkaHttpDependency("akka-parsing"),
        protobufJavaUtil,
        akkaslsProxyProtocol % "protobuf-src",
        akkaslsSdkProtocol % "compile;protobuf-src",
        akkaDependency("akka-testkit") % Test,
        akkaDependency("akka-stream-testkit") % Test,
        akkaHttpDependency("akka-http-testkit") % Test,
        scalaTest % Test,
        logback % "test;provided",
        logbackContrib % Provided,
        jacksonDatabind
      )

  val tck = deps ++= Seq(
        akkaslsTckProtocol % "protobuf-src",
        "com.akkaserverless" % "akkaserverless-tck-protocol" % AkkaServerless.FrameworkVersion % "protobuf-src",
        "ch.qos.logback" % "logback-classic" % LogbackVersion
      )

  val testkit = deps ++= Seq(
        testContainers,
        junit4 % Provided,
        junit5 % Provided
      )

  val codegenCore = deps ++= Seq(
        protobufJava,
        akkaslsSdkProtocol % "compile;protobuf-src",
        logback % Test,
        munit % Test,
        munitScalaCheck % Test
      )

  val codegenJava = deps ++= Seq(
        commonsIo,
        logback % Test,
        munit % Test,
        munitScalaCheck % Test
      )

  val excludeTheseDependencies: Seq[ExclusionRule] = Seq(
    // exclusion rules can be added here
  )

  def akkaDependency(name: String, excludeThese: ExclusionRule*) =
    "com.typesafe.akka" %% name % AkkaVersion excludeAll ((excludeTheseDependencies ++ excludeThese): _*)

  def akkaHttpDependency(name: String, excludeThese: ExclusionRule*) =
    "com.typesafe.akka" %% name % AkkaHttpVersion excludeAll ((excludeTheseDependencies ++ excludeThese): _*)

}
