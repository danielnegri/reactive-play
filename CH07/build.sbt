name := "CH07"

version := "1.0"

lazy val `ch07` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "Spy Repository" at "http://files.couchbase.com/maven2"

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.github.mumoshu" %% "play2-memcached" % "0.6.0",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.jooq" % "jooq" % "3.6.0",
  "org.jooq" % "jooq-codegen-maven" % "3.6.0",
  "org.jooq" % "jooq-meta" % "3.6.0",
  "joda-time" % "joda-time" % "2.7",
  "com.github.ironfish" %% "akka-persistence-mongo-casbah"  % "0.7.6-SNAPSHOT"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

val generateJOOQ = taskKey[Seq[File]]("Generate JooQ classes")

val generateJOOQTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (src, cp, r, s) =>
  toError(r.run("org.jooq.util.GenerationTool", cp.files, Array("conf/chapter7.xml"), s.log))
  ((src / "main/generated") ** "*.scala").get
}

generateJOOQ <<= generateJOOQTask

sourceGenerators in Compile <+= generateJOOQTask