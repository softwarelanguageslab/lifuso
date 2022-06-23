package dictionary

import org.apache.commons.io.FileUtils

import java.io.{File, IOException}
import java.net.URL

class JARFile(groupID: String, artifactID: String, version: String){

  def pathJar(): String = {
    groupID.replaceAll("\\.", "/") + "/" +
      artifactID + "/" +
      version + "/" +
      artifactID + "-" + version + ".jar"
  }

  def downloadJar(path: String, address: String): Unit = {
    if (!new File(path).exists())
      download(path, address)
  }

  def download(target: String, url: String): Unit = {
    println(s"Downloading $url ...")
    val file: File = new File(target)
    try {
      if (!file.exists())
        FileUtils.copyURLToFile(new URL(url), new File(target))
      println("Done !")
    } catch {
      case _: IOException =>
    }
  }

}
