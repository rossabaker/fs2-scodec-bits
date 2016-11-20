lazy val contributors = Seq(
  "rossabaker" -> "Ross A. Baker"
)

organization := "co.fs2"
name := "fs2-scodec-bits"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8", "2.12.0")

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-unused-import"
)
scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)}
scalacOptions in (Test, console) <<= (scalacOptions in (Compile, console))
addCompilerPlugin("org.spire-math" % "kind-projector" % "0.7.1" cross CrossVersion.binary)

resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"
libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "1.0.0-SNAPSHOT",
  "org.scodec" %% "scodec-bits" % "1.1.2",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
)

scmInfo := Some(ScmInfo(url("https://github.com/rossabaker/fs2-scodec-bits"), "git@github.com:rossabaker/fs2-scodec-bits.git"))
homepage := Some(url("https://github.com/functional-streams-for-scala/fs2"))
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

initialCommands := s"""
  import fs2._
  import fs2.util.Task
  import fs2.interop.scodec.bits._
  import scodec.bits.
"""

doctestWithDependencies := false

parallelExecution in Test := false
logBuffered in Test := false
publishArtifact in Test := true

scalacOptions in (Compile, doc) ++= Seq(
  "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
  "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
  "-implicits",
  "-implicits-show-all"
)
scalacOptions in (Compile, doc) ~= (_.filterNot(_ == "-Xfatal-warnings"))
autoAPIMappings := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
credentials ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
publishMavenStyle := true
pomIncludeRepository := { _ => false }
pomExtra := {
  <developers>
    {for ((username, name) <- contributors) yield
    <developer>
      <id>{username}</id>
      <name>{name}</name>
      <url>http://github.com/{username}</url>
    </developer>
    }
  </developers>
}
pomPostProcess := { node =>
  import scala.xml._
  import scala.xml.transform._
  def stripIf(f: Node => Boolean) = new RewriteRule {
    override def transform(n: Node) =
      if (f(n)) NodeSeq.Empty else n
  }
  val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
  new RuleTransformer(stripTestScope).transform(node)(0)
}

releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value

