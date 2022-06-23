package versions

import utils.Library
import requests.Response

class ScrapVersions(library: Library) {

  def scrap(): Library = {
    val url: String = "https://search.maven.org/solrsearch/select?q=g:%22" + library.groupID + "%22%20AND%20a:%22" +
      library.artifactID + "%22&core=gav&rows=50&wt=json"
    val response: Response = requests.get(url)

    if (response.statusCode == 200) {
      val jsonData = ujson.read(response.text())

      val versions: List[String] = jsonData("response")("docs") match {
        case array: ujson.Arr => array.arr.toArray.map(document =>
          document("v").value.toString
        ).toList
        case _ => List()
      }

      library.addVersions(versions)
    } else library
  }
}
