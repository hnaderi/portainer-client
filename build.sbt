import scala.scalanative.build._
// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "dev.hnaderi"
ThisBuild / organizationName := "Hossein Naderi"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("hnaderi", "Hossein Naderi")
)

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

val Scala213 = "2.13.9"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.2.0")
ThisBuild / scalaVersion := Scala213 // the default Scala

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
  .nativeSettings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-curl" % "0.1.1"
    ),
    nativeConfig ~= { _.withGC(GC.none).withLTO(LTO.thin) }
  )

lazy val docs = project.in(file("site")).enablePlugins(TypelevelSitePlugin)
