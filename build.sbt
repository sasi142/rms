name := "rms"

version := sys.env.get("BUILD_TIMESTAMP").getOrElse("") +"_"+ sys.env.get("SVN_REVISION_1").getOrElse("") +"_"+sys.env.get("BUILD_NUMBER").getOrElse("")

 lazy val root = (project in file(".")).
 enablePlugins(BuildInfoPlugin).
 enablePlugins(PlayJava).
 settings( 
  	  buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
      buildInfoPackage := "hello"
 )

//resolvers += "Local Project Directory" at "file:///" + baseDirectory.value + "/lib"
//resolvers ++= Seq(Resolver.jcenterRepo, "Sonatype snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/")

resolvers += "Maven Central Server" at "https://repo1.maven.org/maven2"

//resolvers += "Typesafe Server" at "https://dl.bintray.com/typesafe/maven-releases/"
resolvers += "Typesafe Server" at "https://repo.typesafe.com/typesafe/maven-releases/"

resolvers += "Local Project Directory" at "file:///" + baseDirectory.value + "/lib"

scalaVersion := "2.12.8"

val akkaVersion = "2.5.22"

val springVersion = "5.2.3.RELEASE"

libraryDependencies += guice
libraryDependencies += ws
//libraryDependencies += javaJdbc
libraryDependencies += javaJpa
libraryDependencies += "io.github.jyllands-posten" %% "play-prometheus-filters" % "0.6.1"
libraryDependencies += "org.webjars" %% "webjars-play" % "2.7.0"
libraryDependencies += "org.webjars" % "bootstrap" % "2.3.2"
libraryDependencies += "org.webjars" % "flot" % "0.8.3"
libraryDependencies += "com.google.inject" % "guice" % "3.0"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.9"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % akkaVersion
libraryDependencies += "org.springframework" % "spring-context" % springVersion
libraryDependencies += "org.springframework" % "spring-orm" % springVersion
libraryDependencies += "org.springframework" % "spring-jdbc" % springVersion
libraryDependencies += "org.springframework" % "spring-tx" % springVersion
libraryDependencies += "org.springframework" % "spring-expression" % springVersion
libraryDependencies += "org.springframework" % "spring-aop" % springVersion
libraryDependencies += "org.springframework" % "spring-web" % springVersion
libraryDependencies += "redis.clients" % "jedis" % "3.2.0"
libraryDependencies += "org.springframework" % "spring-test" % springVersion % Test
//libraryDependencies += "redis.clients" % "jedis" % "3.0.1"
libraryDependencies += "com.typesafe" % "play-plugins-redis_2.10" % "2.2.1"
libraryDependencies += "org.apache.commons" % "commons-dbcp2" % "2.7.0"
libraryDependencies += "org.apache.commons" % "commons-text" % "1.8"
//libraryDependencies += "org.hibernate" % "hibernate-entitymanager" % "5.4.6.Final"  
//libraryDependencies += "org.hibernate" % "hibernate-entitymanager" % "5.4.3.Final"
libraryDependencies += "org.hibernate" % "hibernate-core" % "5.4.6.Final"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.21"
//libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"
libraryDependencies += "com.ganyo" % "gcm-server" % "1.1.0"
libraryDependencies += "org.apache.poi" % "poi" % "3.16"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "3.16"
libraryDependencies += "org.bouncycastle" % "bcprov-jdk15on" % "1.62"
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.12.0"
libraryDependencies += "com.github.jedis-lock" % "jedis-lock" % "1.0.0"
libraryDependencies += "org.apache.httpcomponents" % "httpcore" % "4.4.11"
libraryDependencies += "org.apache.httpcomponents" % "httpcore-nio" % "4.4.12"
libraryDependencies += "nl.martijndwars" % "web-push" % "5.0.2"
libraryDependencies += "org.apache.httpcomponents" % "httpasyncclient" % "4.1.4"
//libraryDependencies += "org.elasticsearch.client" % "rest" % "5.5.3"
libraryDependencies += "org.elasticsearch.client" % "elasticsearch-rest-client" % "7.7.0" exclude("org.apache.httpcomponents", "httpclient")
libraryDependencies += "org.jsoup" % "jsoup" % "1.12.1"
//libraryDependencies += "org.apache.velocity" % "velocity" % "1.7"
libraryDependencies += "org.apache.velocity" % "velocity-engine-core" % "2.0"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "com.alibaba.spring" % "spring-context-velocity" % "4.3.18.RELEASE"
libraryDependencies += "dom4j" % "dom4j" % "1.6.1"
libraryDependencies += "com.github.mlaccetti" % "javapns" % "2.3.2"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.22"
libraryDependencies += "commons-validator" % "commons-validator" % "1.6"
libraryDependencies += "org.springframework" % "spring-core" % "5.2.3.RELEASE"
//libraryDependencies += "log4j" % "log4j" % "1.2.14"
libraryDependencies += "net.javacrumbs.shedlock" % "shedlock-provider-redis-jedis" % "4.5.2"
libraryDependencies += "net.javacrumbs.shedlock" % "shedlock-core" % "4.5.2"
libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.12.114"
libraryDependencies += "javax.annotation" % "javax.annotation-api" % "1.3.2"
libraryDependencies += "software.amazon.awssdk" % "mediaconvert" % "2.17.150"