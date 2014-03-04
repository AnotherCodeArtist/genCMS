import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "genCMS"
    val appVersion      = "1.0-SNAPSHOT"

	scalaVersion := "2.10.2"
	
    val appDependencies = Seq(
    	javaCore,
      	//"com.feth" %% "play-easymail" % "0.5-SNAPSHOT",
		"org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
		"ws.securesocial" %% "securesocial" % "2.1.3",
		"com.typesafe" %% "play-plugins-mailer" % "2.1-RC2"
    )
	

	

    val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += Resolver.sonatypeRepo("releases"),
	  resolvers += Resolver.url("play-easymail (release)", url("http://joscha.github.com/play-easymail/repo/releases/"))(Resolver.ivyStylePatterns),
      resolvers += Resolver.url("play-easymail (snapshot)", url("http://joscha.github.com/play-easymail/repo/snapshots/"))(Resolver.ivyStylePatterns),
	  resolvers += Resolver.url("sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
	  resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
	  //resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/org/reactivemongo/play2-reactivemongo_2.10/0.10.0-SNAPSHOT/"
    )

}