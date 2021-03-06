// Rip from scalatra build file

package mbench

import sbt._
import sbt.Keys._
import ls.Plugin.LsKeys
import com.typesafe.sbt.SbtScalariform
import com.typesafe.tools.mima.plugin.MimaPlugin

object Build extends Build {

  import Dependencies._

  import Repos.sonatypeNexusSnapshots

  // Helpers
  def projectId(state: State) = extracted(state).currentProject.id
  def extracted(state: State) = Project extract state

  lazy val buildSettings:Seq[Setting[_]] = Seq(
    organization       := "com.github.sbocq",
	version            := "0.2.4",
	manifestSetting,
    crossScalaVersions := Seq("2.9.3", "2.10.1", "2.10.2", "2.10.3"),
    scalaVersion       := "2.10.3",
    scalacOptions      ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8"),
    scalacOptions      ++= Seq(), //Seq("-language:higherKinds", "-language:postfixOps", "-language:implicitConversions", "-language:reflectiveCalls", "-language:existentials"),
    javacOptions       ++= Seq("-target", "1.6", "-source", "1.6", "-Xlint:deprecation"),
    resolvers          ++= Seq(sonatypeNexusSnapshots),
	shellPrompt        := { "sbt (%s)> " format projectId(_) },
    (LsKeys.tags in LsKeys.lsync)    := Seq("mbench")
  )

  lazy val manifestSetting:Setting[_] = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor,
		"Sealed" -> "true"
      )
  }

  lazy val sharedSettings =
    Defaults.defaultSettings ++
	ls.Plugin.lsSettings     ++
	Collect.settings         ++
	SbtScalariform.scalariformSettings ++
	MimaPlugin.mimaDefaultSettings ++
	Publish.settings ++
	buildSettings

  lazy val runSettings =
          Seq(
          scalacOptions <++= (scalaVersion).map(sv =>
              if (sv.startsWith("2.9")) Seq("-Ydependent-method-types") else Seq.empty[String]),
	  fork := true,
	  fork in test := true,
	  javaOptions <++= (fullClasspath in Runtime).map(cp => Seq("-cp", cp.files.mkString(System.getProperty("path.separator")), "-server", "-Dmbench.log.stdout=true", "-Dmbench.new.date.dir=sbtrun")),
      libraryDependencies <++= scalaVersion(sv => Seq(
        Test.scalatest(sv)
      ))
	)

  lazy val doNotPublish = Seq(publish := {}, publishLocal := {})

  lazy val mbenchProject = Project(
    id = "mbench-project",
    base = file("."),
    settings = sharedSettings ++ Site.settings ++ Collect.doNotCollect ++ doNotPublish ++ Seq(
      description := "A benchmarking tool for the JVM",
      Unidoc.unidocExclude := Seq("mbench-benchmarks"),
      LsKeys.skipWrite := true
    ),
    aggregate = Seq(mbench, mbenchBenchmarks)
  )

  lazy val mbench = Project(
    id = "mbench",
    base = file("mbench"),
    settings = sharedSettings ++ Seq(
	  description := "A benchmarking tool for the JVM"
	)
  )

  lazy val mbenchBenchmarks = Project(
     id = "mbench-benchmarks",
     base = file("mbench-benchmarks"),
     settings = sharedSettings ++ runSettings ++ Seq(
       description := "A standard benchmark suite"
     )
  ) dependsOn(mbench % "compile;test->test")


}
