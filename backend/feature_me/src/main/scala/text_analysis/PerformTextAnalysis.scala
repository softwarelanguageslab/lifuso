package text_analysis

object PerformTextAnalysis {

  /** Performs several operations on a text passed as an argument.
   *
   * @param text The text to be transformed by the operations.
   * @param performStemming A boolean indicating whether the stemming operation should be performed.
   * @return A transformed version of the text.
   * @note Check the tests for a more concrete example.
   */
  def performTextAnalysis(text: List[String], performStemming: Boolean = true): List[String] = {
    val operations: List[List[String] => List[String]] =
      if (performStemming)
        List(TextAnalysis.noSymbols, TextAnalysis.lowering, TextAnalysis.stopWords, TextAnalysis.stemming)
      else
        List(TextAnalysis.noSymbols, TextAnalysis.lowering, TextAnalysis.stopWords)

    TextAnalysis.processText(text, operations)
  }

}
