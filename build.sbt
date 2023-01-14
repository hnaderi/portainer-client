import scala.scalanative.build._
ThisBuild / tlBaseVersion := "0.0"

ThisBuild / organization := "dev.hnaderi"
ThisBuild / organizationName := "Hossein Naderi"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("hnaderi", "Hossein Naderi")
)

ThisBuild / tlSonatypeUseLegacyHost := false

// TODO: This section is a mindless copy paste from http4s-curl
// which might need some tweaks?!
ThisBuild / githubWorkflowOSes :=
  Seq("ubuntu-20.04", "ubuntu-22.04", "macos-11", "macos-12", "windows-2022")
ThisBuild / githubWorkflowBuildMatrixExclusions +=
  MatrixExclude(Map("scala" -> Scala3, "os" -> "windows-2022")) // dottydoc bug

//To help with rerunning failed steps
ThisBuild / githubWorkflowBuildMatrixFailFast := Some(false)

val vcpkgBaseDir = "C:/vcpkg/"
ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Run(
    List("sudo apt-get update", "sudo apt-get install libcurl4-openssl-dev"),
    name = Some("Install libcurl (ubuntu)"),
    cond = Some("startsWith(matrix.os, 'ubuntu')")
  ),
  WorkflowStep.Run(
    List(
      "vcpkg integrate install",
      "vcpkg install --triplet x64-windows curl",
      """cp "C:\vcpkg\installed\x64-windows\lib\libcurl.lib" "C:\vcpkg\installed\x64-windows\lib\curl.lib""""
    ),
    name = Some("Install libcurl (windows)"),
    cond = Some("startsWith(matrix.os, 'windows')")
  )
)
ThisBuild / githubWorkflowBuildPostamble ~= {
  _.filterNot(
    _.name.contains("Check unused compile dependencies")
  )
}

ThisBuild / nativeConfig ~= { c =>
  if (BuildEnv.isMacOs) { // brew-installed curl
    c.withLinkingOptions(c.linkingOptions :+ "-L/usr/local/opt/curl/lib")
  } else if (BuildEnv.isWindows) { // vcpkg-installed curl
    c.withCompileOptions(
      c.compileOptions :+ s"-I${vcpkgBaseDir}/installed/x64-windows/include/"
    ).withLinkingOptions(
      c.linkingOptions :+ s"-L${vcpkgBaseDir}/installed/x64-windows/lib/"
    )
  } else c
}
ThisBuild / envVars ++= {
  if (sys.props.get("os.name").exists(_.toLowerCase().contains("windows")))
    Map(
      "PATH" -> s"${sys.props.getOrElse("PATH", "")};${vcpkgBaseDir}/installed/x64-windows/bin/"
    )
  else Map.empty[String, String]
}
/////

val Scala213 = "2.13.10"
val Scala3 = "3.2.1"
ThisBuild / crossScalaVersions := Seq(Scala213, Scala3)
ThisBuild / scalaVersion := Scala213

lazy val root = tlCrossRootProject
  .aggregate(core, cli)
  .settings(
    name := "portainer"
  )

val catsEffectVersion = "3.3.14"
val http4sVersion = "0.23.16"
val munitCEVersion = "2.0.0-M3"

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "portainer-client",
    description := "portainer client library",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-effect" % catsEffectVersion,
      "org.http4s" %%% "http4s-client" % http4sVersion,
      "org.http4s" %%% "http4s-circe" % http4sVersion,
      "org.typelevel" %%% "munit-cats-effect" % munitCEVersion % Test
    )
  )

lazy val cli = crossProject(JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("cli"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core)
  .settings(
    name := "portainer-cli",
    description := "portainer client cli",
    libraryDependencies ++= Seq(
      "com.monovore" %%% "decline" % "2.4.1",
      "org.typelevel" %%% "munit-cats-effect" % munitCEVersion % Test,
      "dev.hnaderi" %%% "readpassword" % "0.0.1"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-ember-client" % http4sVersion
    )
  )
  .nativeSettings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-curl" % "0.1.1"
    ),
    nativeConfig ~= { old =>
      val temp = old.withGC(GC.none)
      if (sys.env.contains("RELEASE") || true)
        temp
          // NOTE https://github.com/scala-native/scala-native/issues/2779
          // due to some linker problem on macos, LTO is disabled conditionally
          .withLTO(if (BuildEnv.isMacOs) LTO.none else LTO.thin)
          .withMode(Mode.releaseFast)
      else temp
    },
    nativeLinkingOptions ++= Seq(
      "-static",
      "-v",
      "-s", "-w"
    )
