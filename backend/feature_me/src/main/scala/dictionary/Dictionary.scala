package dictionary

import utils.Library

import org.apache.bcel.classfile.{ClassParser, JavaClass, Method}
import upickle.default.write

import java.io.{File, PrintWriter}
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters._
import scala.collection.mutable.{Map => MMap}

class Dictionary(library: Library, dataFolder: String) {

  val MAVEN_ADDRESS: String = "https://repo1.maven.org/maven2/"

  def createDictionary(): Unit = {
    val dictionaryLibrary: MMap[String, Array[String]] = MMap()

    library.versions.getOrElse(List()).foreach(version => {
      println(s"Analysing triplet ${library.groupID} -> ${library.artifactID} -> $version")

      // 1. Download the JAR file with a triple combination
      val jarFile: JARFile = new JARFile(library.groupID, library.artifactID, version)
      val pathJarStr: String = jarFile.pathJar()
      val path: String = s"$dataFolder/jars/" + pathJarStr
      jarFile.downloadJar(path, MAVEN_ADDRESS + pathJarStr)

      // 2. Use BCEL to extract class and method names and
      // 3. Add class or method names to a library dictionary if such values are not there already
      increaseDictionary(path, dictionaryLibrary)
    })

    // Save the data as a JSON file per library
    println("Writing dictionary for the library ...")

    val jsonToWrite: String = write(dictionaryLibrary)
    val pw: PrintWriter = new PrintWriter(new File(s"$dataFolder/dictionaries/${library.groupID}" +
      s"|${library.artifactID}.json"))

    pw.write(jsonToWrite)
    pw.close()

    println("Done!")
  }

  def increaseDictionary(filePath: String, dictionary: MMap[String, Array[String]]): MMap[String, Array[String]] = {
    val file: File = new File(filePath)

    if (file.exists()) {
      val zipFile = new ZipFile(file)

      val entries = zipFile.entries.asScala
      entries.filter(x => !x.isDirectory && x.getName.endsWith(".class")).foreach(entry => {
        val entryName: String = entry.getName

        if (!entryName.equals("module-info.class")) {
          val classParser: ClassParser = new ClassParser(zipFile.getInputStream(entry), entry.getName)
          val javaClass: JavaClass = classParser.parse()

          if (javaClass.isPublic) {
            val className: String = entryName.split("/").last.split("\\.").head

            val methods: Array[Method] = javaClass.getMethods
            val publicMethodNames: Array[String] = methods.filter(_.isPublic()).map(_.getName()).distinct

            if (dictionary.contains(className)) {
              val currentMethods: Array[String] = dictionary(className)
              dictionary(className) = currentMethods.concat(publicMethodNames).distinct
            } else {
              dictionary(className) = publicMethodNames
            }
          }
        }
      })
      zipFile.close()
    }
    dictionary
  }

}
