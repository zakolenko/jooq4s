lazy val jooqVersion     = "3.12.0"
lazy val hikariVersion   = "3.4.1"
lazy val fs2Version      = "2.0.1"
lazy val scala212Version = "2.12.10"

lazy val commonSettings = Seq(
  version := "0.1.0",
  organization := "com.zakolenko",
  scalaVersion := scala212Version,
  licenses ++= Seq()
)

lazy val jooq4s =
  project
    .in(file("."))
    .settings(commonSettings)
    .aggregate(
      core,
      hikari
    )

lazy val core =
  project
    .in(file("core"))
    .settings(commonSettings)
    .settings(
      name := moduleName("core"),
      description := "",
      libraryDependencies ++= Seq(
        "org.jooq" %% "jooq-scala" % jooqVersion,
        "co.fs2" %% "fs2-core" % fs2Version
      )
    )

lazy val hikari =
  project
    .in(file("hikari"))
    .dependsOn(core)
    .settings(commonSettings)
    .settings(
      name := moduleName("hikari"),
      description := "",
      libraryDependencies ++= Seq(
        "com.zaxxer" % "HikariCP" % hikariVersion
      )
    )

def moduleName(x: String): String = s"jooq4s-$x"