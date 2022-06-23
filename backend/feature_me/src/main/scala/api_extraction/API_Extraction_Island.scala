package api_extraction

import com.parser.extractor.Extractor
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

object API_Extraction_Island {
  def extractCode(body: String): List[String] = {
    val html: Document = Jsoup.parse(body)
    val codeElements: Elements = html.select("code")

    // Each element in the array of codes could be considered as a snippet of nonImportCode
    codeElements.toArray().map(_.asInstanceOf[Element].text()).toList
  }

  def extractAPIs(codeSnippet: String): List[String] = {
    val extractor: Extractor = new Extractor(codeSnippet)
    val methodInvocations = extractor.extractMethodCalls()
    var methodInvocationsArray: Array[String] = Array()

    methodInvocations.forEach(call => {
      methodInvocationsArray :+= call
    })

    methodInvocationsArray.toList
  }
}
