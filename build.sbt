import Dependencies._

lazy val `akkaserverless-java-sdk` = project
  .in(file("."))
  .aggregate(
    sdk,
    testkit,
    tck,
    codegenCore,
    codegenJava,
    codegenJavaCompilationTest,
    samples
  )

lazy val sdk = project
  .in(file("sdk"))
  .enablePlugins(AkkaGrpcPlugin, BuildInfoPlugin, PublishSonatype)
  .settings(
    name := "akkaserverless-java-sdk",
    crossPaths := false,
    buildInfoKeys := Seq[BuildInfoKey](
        name,
        version,
        "protocolMajorVersion" -> AkkaServerless.ProtocolVersionMajor,
        "protocolMinorVersion" -> AkkaServerless.ProtocolVersionMinor,
        "scalaVersion" -> scalaVersion.value
      ),
    buildInfoPackage := "com.akkaserverless.javasdk",
    // Generate javadocs by just including non generated Java sources
    Compile / doc / sources := {
      val javaSourceDir = (Compile / javaSource).value.getAbsolutePath
      (Compile / doc / sources).value.filter(_.getAbsolutePath.startsWith(javaSourceDir))
    },
    // javadoc (I think java 9 onwards) refuses to compile javadocs if it can't compile the entire source path.
    // but since we have java files depending on Scala files, we need to include ourselves on the classpath.
    Compile / doc / dependencyClasspath := (Compile / fullClasspath).value,
    Compile / doc / javacOptions ++= Seq(
        "-Xdoclint:none",
        "-overview",
        ((Compile / javaSource).value / "overview.html").getAbsolutePath,
        "--no-module-directories",
        "-notimestamp",
        "-doctitle",
        "Akka Serverless Java SDK",
        "-noqualifier",
        "java.lang"
      ),
    Compile / javacOptions ++= Seq("-encoding", "UTF-8"),
    Compile / compile / javacOptions ++= Seq("--release", "8"),
    Compile / scalacOptions ++= Seq("-release", "8"),
    Compile / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Server),
    Compile / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala), // FIXME should be Java, but here be dragons
    // We need to generate the java files for things like entity_key.proto so that downstream libraries can use them
    // without needing to generate them themselves
    Compile / PB.targets += PB.gens.java -> crossTarget.value / "akka-grpc" / "main",
    Test / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
    Test / PB.protoSources ++= (Compile / PB.protoSources).value,
    Test / PB.targets += PB.gens.java -> crossTarget.value / "akka-grpc" / "test"
  )
  .settings(Dependencies.sdk)

lazy val testkit = project
  .in(file("testkit"))
  .dependsOn(sdk)
  .enablePlugins(BuildInfoPlugin, PublishSonatype)
  .settings(
    name := "akkaserverless-java-sdk-testkit",
    crossPaths := false,
    buildInfoKeys := Seq[BuildInfoKey](
        name,
        version,
        "proxyImage" -> "gcr.io/akkaserverless-public/akkaserverless-proxy",
        "proxyVersion" -> AkkaServerless.FrameworkVersion,
        "scalaVersion" -> scalaVersion.value
      ),
    buildInfoPackage := "com.akkaserverless.javasdk.testkit",
    Compile / javacOptions ++= Seq("-encoding", "UTF-8"),
    Compile / compile / javacOptions ++= Seq("--release", "8"),
    Compile / scalacOptions ++= Seq("-release", "8"),
    // Produce javadoc by restricting to Java sources only -- no genjavadoc setup currently
    Compile / doc / sources := (Compile / doc / sources).value.filterNot(_.name.endsWith(".scala"))
  )
  .settings(Dependencies.testkit)

lazy val tck = project
  .in(file("tck"))
  .dependsOn(sdk, testkit % Test)
  .enablePlugins(AkkaGrpcPlugin, PublicDockerImage)
  .settings(
    name := "akkaserverless-tck-java-sdk",
    akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
    Compile / mainClass := Some("com.akkaserverless.javasdk.tck.JavaSdkTck"),
    Compile / javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11"),
    dockerEnvVars += "HOST" -> "0.0.0.0",
    dockerExposedPorts += 8080
  )
  .settings(Dependencies.tck)

lazy val codegenCore =
  project
    .in(file("codegen/core"))
    .enablePlugins(AkkaGrpcPlugin, PublishSonatype)
    .settings(
      name := "akkaserverless-codegen-core",
      testFrameworks += new TestFramework("munit.Framework"),
      Test / fork := false
    )
    .settings(Dependencies.codegenCore)
    .settings(
      Compile / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java)
    )
    .settings(crossScalaVersions := Dependencies.ScalaVersionForCodegen,
              scalaVersion := Dependencies.ScalaVersionForCodegen.head)

lazy val codegenJava =
  project
    .in(file("codegen/java-gen"))
    .configs(IntegrationTest)
    .dependsOn(codegenCore)
    .enablePlugins(PublishSonatype)
    .settings(Defaults.itSettings)
    .settings(name := "akkaserverless-codegen-java", testFrameworks += new TestFramework("munit.Framework"))
    .settings(Dependencies.codegenJava)
    .settings(crossScalaVersions := Dependencies.ScalaVersionForCodegen,
              scalaVersion := Dependencies.ScalaVersionForCodegen.head)

lazy val codegenJavaCompilationTest = project
  .in(file("codegen/java-gen-compilation-tests"))
  .enablePlugins(ReflectiveCodeGen)
  .dependsOn(sdk)
  // code generated by the codegen requires the testkit, junit4
  // Note: we don't use test scope since all code is generated in src_managed
  // and the goal is to verify if it compiles
  .dependsOn(testkit)
  .settings(libraryDependencies ++= Seq(Dependencies.junit4))
  .settings(
    (publish / skip) := true,
    name := "akkaserverless-codegen-java-compilation-tests",
    Compile / javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11")
  )

lazy val samples = project
  .in(file("samples"))
  .aggregate( // samples relying on codegen must use Maven
             // FIXME consider including this sample again?
             // `java-eventing-shopping-cart`,
             // `valueentity-counter`
  )

/**
 * This sample can be built either with maven
 * (for the 'most faithful' simulation of what it would work like for a user)
 * or via sbt (for fast feedback on SDK changes).
 *
 * This is tricky because it relies on generated code.
 * Generating the code 'inside' the sbt build is tricky
 * because the codegen code itself is an sbt project as well.
 * We use something like this in Akka gRPC (ReflectiveCodeGen)
 * but that introduces quite some problems, so perhaps we should
 * keep it simpler: checking in the generated in a separate place
 * and regenerating it manually.
 */
lazy val `valueentity-counter` = project
  .in(file("samples/valueentity-counter"))
  .disablePlugins(JavaFormatterPlugin, HeaderPlugin)
  .dependsOn(sdk, testkit % Test)
  .settings(
    name := "valueentity-counter",
    Compile / unmanagedSourceDirectories += baseDirectory.value / ".." / "valueentity-counter-generated" / "src" / "main" / "java",
    Test / unmanagedSourceDirectories += baseDirectory.value / ".." / "valueentity-counter-generated" / "src" / "test" / "java",
    libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
        "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
        "junit" % "junit" % "4.13.1" % Test
      )
  )

lazy val `java-eventing-shopping-cart` = project
  .in(file("samples/java-eventing-shopping-cart"))
  .dependsOn(sdk, testkit % IntegrationTest)
  .enablePlugins(AkkaGrpcPlugin, IntegrationTests, LocalDockerImage)
  .settings(
    name := "java-eventing-shopping-cart",
    Compile / mainClass := Some("shopping.Main"),
    // Akka gRPC only for IntegrationTest
    Compile / akkaGrpcGeneratedSources := Seq.empty,
    Compile / javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11"),
    libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
        "com.novocode" % "junit-interface" % JUnitInterfaceVersion % IntegrationTest
      ),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-a"),
    IntegrationTest / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
    IntegrationTest / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
    IntegrationTest / PB.protoSources ++= (Compile / PB.protoSources).value
  )
  .settings(attachProtobufDescriptorSets)

lazy val `java-valueentity-customer-registry` = project
  .in(file("samples/java-valueentity-customer-registry"))
  .dependsOn(sdk)
  .enablePlugins(AkkaGrpcPlugin, IntegrationTests, LocalDockerImage)
  .settings(
    name := "java-valueentity-customer-registry",
    libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
        "ch.qos.logback.contrib" % "logback-json-classic" % LogbackContribVersion,
        "ch.qos.logback.contrib" % "logback-jackson" % LogbackContribVersion,
        "org.junit.jupiter" % "junit-jupiter" % JUnitJupiterVersion % IntegrationTest,
        "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % IntegrationTest
      ),
    Compile / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
    Compile / javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11"),
    testOptions += Tests.Argument(jupiterTestFramework, "-q", "-v"),
    inConfig(IntegrationTest)(JupiterPlugin.scopedSettings),
    IntegrationTest / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
    IntegrationTest / PB.protoSources ++= (Compile / PB.protoSources).value
  )

lazy val `java-eventsourced-customer-registry` = project
  .in(file("samples/java-eventsourced-customer-registry"))
  .dependsOn(sdk)
  .enablePlugins(AkkaGrpcPlugin, IntegrationTests, LocalDockerImage)
  .settings(
    name := "java-eventsourced-customer-registry",
    libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % LogbackVersion,
        "ch.qos.logback.contrib" % "logback-json-classic" % LogbackContribVersion,
        "ch.qos.logback.contrib" % "logback-jackson" % LogbackContribVersion,
        "org.junit.jupiter" % "junit-jupiter" % JUnitJupiterVersion % IntegrationTest,
        "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % IntegrationTest
      ),
    Compile / akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
    Compile / javacOptions ++= Seq("-encoding", "UTF-8", "-source", "11", "-target", "11"),
    testOptions += Tests.Argument(jupiterTestFramework, "-q", "-v"),
    inConfig(IntegrationTest)(JupiterPlugin.scopedSettings),
    IntegrationTest / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
    IntegrationTest / PB.protoSources ++= (Compile / PB.protoSources).value
  )

lazy val protobufDescriptorSetOut = settingKey[File]("The file to write the protobuf descriptor set to")

lazy val attachProtobufDescriptorSets = Seq(
  protobufDescriptorSetOut := (Compile / resourceManaged).value / "protobuf" / "descriptor-sets" / "user-function.desc",
  Compile / PB.generate := (Compile / PB.generate)
      .dependsOn(Def.task {
        protobufDescriptorSetOut.value.getParentFile.mkdirs()
      })
      .value,
  Compile / PB.targets := Seq(PB.gens.java -> (Compile / sourceManaged).value),
  Compile / PB.protocOptions ++= Seq(
      "--descriptor_set_out",
      protobufDescriptorSetOut.value.getAbsolutePath,
      "--include_source_info"
    ),
  Compile / managedResources += protobufDescriptorSetOut.value,
  Compile / unmanagedResourceDirectories ++= (Compile / PB.protoSources).value
)
