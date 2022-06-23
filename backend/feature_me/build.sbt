name := "feature_me"
version := "0.1"
scalaVersion := "2.13.5"

unmanagedJars in Compile ++= Seq(
  file("lib/islandParser.jar")
)

libraryDependencies ++= Seq(
  // Utilities
  "com.lihaoyi" %% "requests" % "0.6.5",
  "com.lihaoyi" %% "ujson" % "1.2.3",
  "com.lihaoyi" %% "upickle" % "1.2.3",
  "commons-io" % "commons-io" % "2.8.0",

  // Read the CSVs
  "org.apache.commons" % "commons-csv" % "1.8",

  // Extract information from JARs
  "org.apache.bcel" % "bcel" % "6.5.0",

  // Extract code from HTML
  "org.jsoup" % "jsoup" % "1.13.1",

  // NLP Operations
  "org.apache.opennlp" % "opennlp-tools" % "1.9.3",
  "edu.stanford.nlp" % "stanford-corenlp" % "4.2.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "4.2.0" classifier "models",

  // Testing
  "org.scalatest" %% "scalatest" % "3.2.6" % "test",
  "org.scalatest" %% "scalatest-flatspec" % "3.2.6" % "test",

  // DeepLearning4j
  "org.deeplearning4j" % "deeplearning4j-core" % "1.0.0-beta7",
  "org.deeplearning4j" % "deeplearning4j-nlp" % "1.0.0-beta7",
  "org.nd4j" % "nd4j-native" % "1.0.0-beta7",

  // No logs
  "org.slf4j" % "slf4j-nop" % "2.0.0-alpha1",
)
