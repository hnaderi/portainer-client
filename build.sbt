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
ThisBuild / tlSitePublishBranch := Some("main")

val Scala213 = "2.13.9"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.2.0")
ThisBuild / scalaVersion := Scala213

lazy val root = tlCrossRootProject.aggregate(core, cli)

val catsEffectVersion = "3.3.14"
val http4sVersion = "0.23.16"
val munitCEVersion = "2.0.0-M3"

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "portainer-cli",
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
    libraryDependencies ++= Seq(
      "com.monovore" %%% "decline" % "2.4.1"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-ember-client" % http4sVersion
    ),
  )
  .nativeSettings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-curl" % "0.1.1"
    ),
    nativeConfig ~= { _.withGC(GC.none)// .withLTO(LTO.thin).withMode(Mode.releaseFull)
    }
  )

lazy val docs = project.in(file("site")).enablePlugins(TypelevelSitePlugin)
