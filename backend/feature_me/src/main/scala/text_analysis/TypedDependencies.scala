package text_analysis

import edu.stanford.nlp.pipeline.{CoreDocument, StanfordCoreNLP}
import edu.stanford.nlp.semgraph.SemanticGraph

import java.util.Properties

object TypedDependencies {

  /** Obtains the root of a sentence and/or its direct dependencies.
   *
   * @param sentence The sentence to be processed by the algorithm.
   * @param artifactName The name of the artifact to be excluded from the analysis.
   * @param isTitle If the sentence to be analysed is the title of the post.
   *                Titles receive more relevance.
   * @return A simplification of the sentence.
   *         Such a simplification has to be the most important element(s).
   */
  def getRelevantWords(sentence: String, artifactName: String, isTitle: Boolean = false): List[String] = {
    val props: Properties = new Properties()
    props.setProperty("annotators", "tokenize,ssplit,pos,depparse,lemma")

    val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)
    val document: CoreDocument = new CoreDocument(sentence)
    pipeline.annotate(document)

    var elements: Array[String] = Array()

    val transformTag: String => String = (originalTag: String) => {
      if (originalTag.startsWith("NN"))
        "NN"
      else "VB"
    }

    // If the sentence analysed is a title, then increase the frequency of the words on it (more importance)
    val exponentialFactor: Int = if (isTitle) 5 else 1

    document.sentences.forEach(sentence => {
      val dependencyParseTree: SemanticGraph = sentence.dependencyParse()
      dependencyParseTree.typedDependencies().forEach(dependency => {
        val governor_tag: String = dependency.gov().tag()
        val dependency_tag: String = dependency.dep().tag()

        if (governor_tag != null) {
          if ((governor_tag.startsWith("NN") && dependency_tag.startsWith("VB")) ||
              (governor_tag.startsWith("VB") && dependency_tag.startsWith("NN"))) {

            // Lemmas of the original words
            val wordGovernor: String = dependency.gov().lemma()
            val wordDependency: String = dependency.dep().lemma()

            // The types of the original words
            val typeGovernor: String = transformTag(governor_tag)
            val typeDependency: String = transformTag(dependency_tag)

            // Divided by the CamelCase style
            val governorWords: List[String] = TextAnalysis.splitCamelCaseWord(wordGovernor)
            val dependencyWords: List[String] = TextAnalysis.splitCamelCaseWord(wordDependency)

            // Separating the words by dots if any
            val governorWordsSplit: List[String] = governorWords.flatMap(word => {
              word.split("\\.")
            })

            val dependencyWordsSplit: List[String] = dependencyWords.flatMap(word => {
              word.split("\\.")
            })

            // Associating tags with divided terms
            val elementGovernor: String = governorWordsSplit
              .map(_.toLowerCase)
              .filterNot(_.equals(artifactName))
              .map(word => s"$word|$typeGovernor"
            ).mkString("|")

            val elementDependency: String = dependencyWordsSplit
              .map(_.toLowerCase)
              .filterNot(_.equals(artifactName))
              .map(word => s"$word|$typeDependency"
            ).mkString("|")

            val element: String = elementGovernor + "|" + elementDependency
            val elementsConcatenated: IndexedSeq[String] = for (_ <- 0 until exponentialFactor) yield element
            elements :+= elementsConcatenated.mkString(",")
          }
        }
      })
    })

    elements.toList
  }

}
