package text_analysis

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import opennlp.tools.stemmer.PorterStemmer
import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.postag.POSTaggerME

import scala.io.Source

object TextAnalysis {

  /** Divides the tag sentences into a list of tags.
   *
   * @param tags The tag sentence to divide.
   * @return A list of tags composing the tag sentence.
   */
  def chunkTags(tags: String): List[String] = {
    tags.map(chr => {
      if (chr == '<' || chr == '>')
        ' '
      else chr
    })
      .split(" ")
      .map(_.trim)
      .filter(_.nonEmpty)
      .toList
  }

  /** Performs several operations on a text.
   * Operations can be defined as functions and are applied sequentially to the text.
   *
   * @param text The text to be processed by the operations.
   * @param operations The operations in form of functions to be applied to the text.
   * @return A transformed version of the text after the application of the operations.
   */
  def processText(text: List[String], operations: List[List[String] => List[String]]): List[String] = {
    var textTransformed: List[String] = text
    operations.foreach(operation => textTransformed = operation(textTransformed))
    textTransformed
  }

  /** Extracts the non-code part from a code snippet.
   *
   * @param body The body of the code snippet from which the text needs to be extracted.
   * @return The elements on the code snippet with no code on them.
   *         Diverse elements can be returned instead of just one monolithic text.
   */
  def extractBodyNoCode(body: String): List[String] = {
    val html: Document = Jsoup.parse(body)
    // Select all paragraph elements
    val nonCodeElements: Elements = html.select("p")

    // For every element, remove the code part if it has any
    var cleanedElements: Array[String] = Array()
    for (i <- 0 until nonCodeElements.size()) {
      val element: Element = nonCodeElements.get(i)
      element.select("code").remove()
      cleanedElements :+= element.text()
    }

    // Each element in the array of codes could be considered as a snippet of nonImportCode
    cleanedElements
      .mkString(" ")
      .split(" ")
      .toList
  }

  /** Divides a word by its camel case style.
   *
   * @param word The word to be processed.
   * @return A list from which its elements correspond to each case in the camel-case style.
   *
   * @example
   *         {{{
   *           scala>splitCamelCase("ThisIsAnExample")
   *           res: List("This", "Is", "An", "Example")
   *         }}}
   */
  def splitCamelCaseWord(word: String): List[String] = {
    val regexExpression: String = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"

    word.split(regexExpression).toList
  }

  /** Transforms the text by dividing camel case words.
   * Extends the camel case version for a single word:
   * @see splitCamelCaseWord
   *
   * @param text The text to be transformed.
   * @return A camel-case divided version of the text passed as arguments.
   */
  def splitCamelCaseText(text: List[String]): List[String] = {
    text.map(word => splitCamelCaseWord(word).mkString(" "))
      .mkString(" ").split(" ").toList
  }

  /** Cleanse the text by excluding non-desired symbols (e.g., punctuations, parenthesis, etc.)
   *
   * @param text The text to be processed.
   * @return A cleaned version of the text passed as parameter.
   */
  def noSymbols(text: List[String]): List[String] = {
      text.mkString(" ").map(chr => {
      if (!chr.isLetter)
        ' '
      else chr
    }).split(" ")
      .map(_.trim)
      .filter(_.length > 1)
      .toList
  }

  /** Lowers the words passed as parameters.
   *
   * @param text The text to be lowered.
   * @return A lowered version of the text.
   */
  def lowering(text: List[String]): List[String] = {
    text.map(_.toLowerCase)
  }

  /** Filters out the words in the stop words list.
   *
   * @param text The text to be processed.
   * @return A cleaned version of the text without the stop words defined in the list.
   */
  def stopWords(text: List[String]): List[String] = {
    val sourceStopWords: Source = Source.fromFile("src/main/resources/stopwords.txt")
    val stopWords: List[String] = sourceStopWords.getLines().toList
    sourceStopWords.close()

    text.filterNot(word => stopWords.contains(word))
  }

  /** Stems the words passed in the text as arguments.
   *
   * @param text The text to be processed.
   * @return A stemmed version of the words in the text.
   */
  def stemming(text: List[String]): List[String] = {
    val stemmer: PorterStemmer = new PorterStemmer
    text.map(word => stemmer.stem(word))
  }

  /** Extracts the lemmas of the words passed in the text as arguments.
   *
   * @param text The text to be processed.
   * @param posTagger A configured POS model.
   * @param dictionaryLemmatizer A dictionary with the lemmatized words.
   * @return A lemmatized version of the words in the text.
   */
  def lemmatizing(text: List[String],
                  posTagger: POSTaggerME,
                  dictionaryLemmatizer: DictionaryLemmatizer): List[String] = {
    // Applying model
    val tagsText: Array[String] = posTagger.tag(text.toArray)
    val lemmatizedText: List[String] = dictionaryLemmatizer.lemmatize(text.toArray, tagsText).toList

    var fixedText: Array[String] = Array()

    for (i <- lemmatizedText.indices) {
      if (lemmatizedText(i).equals("O")) {
        fixedText :+= text(i)
      } else fixedText :+= lemmatizedText(i)
    }

    fixedText.toList
  }

  /** Filters the words of the text which do not represent some valuable information to consider.
   * The procedure should exclude related to (this is not an exhaustive list):
   * library names, programming languages, IDEs, framework names, operating systems, etc.
   *
   * @param text The text to be filtered.
   * @param artifactID The name of the artifactID of the library.
   * @return A filtered representation of the text.
   */
  def filtering(text: List[String], artifactID: String): List[String] = {
    // The patterns need to be static or related to information already present on the processing
    // otherwise the pipeline will be extremely difficult to execute, even more to explain.
    val verbsExclusion: List[String] = List("use", "be")

    val staticPatterns: List[String] = List("java", "c#")
      .concat(noSymbols(List(artifactID)))
      .concat(verbsExclusion)

    val allCapsExpr: String = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z])"

    val textNoSymbols: List[String] = noSymbols(text)
    textNoSymbols
      .filter(_.length > 2)
      .filterNot(_.startsWith("'"))
      .filterNot(_.contains("."))
      .filterNot(word => splitCamelCaseWord(word).length > 1)
      .filterNot(word => word.split(allCapsExpr).length > 1)
      .map(_.toLowerCase)
      .filterNot(word => staticPatterns.contains(word))
  }
}
