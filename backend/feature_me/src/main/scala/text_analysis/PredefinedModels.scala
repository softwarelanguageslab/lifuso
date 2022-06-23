package text_analysis

import opennlp.tools.lemmatizer.DictionaryLemmatizer
import opennlp.tools.postag.{POSModel, POSTaggerME}

import java.io.{FileInputStream, InputStream}

/** Object to load in memory some of the predefined models.
 * Convenient for optimization on execution.
 */
object PredefinedModels {

  /** Loads the defined models to perform a lemmatization on the text.
   * @return A tuple containing the posTagger and lemmatizer models.
   */
  def lemmatization(): (POSTaggerME, DictionaryLemmatizer) = {
    // Loading POS Tagger model
    val streamPOS: InputStream = new FileInputStream("lib/en-pos-maxent.bin")
    val posModel: POSModel = new POSModel(streamPOS)
    val posTagger: POSTaggerME = new POSTaggerME(posModel)

    // Loading Lemmatizer
    val streamLemmatizer: InputStream = new FileInputStream("lib/en-lemmatizer.txt")
    val dictionaryLemmatizer: DictionaryLemmatizer = new DictionaryLemmatizer(streamLemmatizer)

    (posTagger, dictionaryLemmatizer)
  }

}
