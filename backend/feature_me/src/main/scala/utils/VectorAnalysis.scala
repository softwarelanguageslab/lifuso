package utils

import org.deeplearning4j.bagofwords.vectorizer.{BagOfWordsVectorizer, TfidfVectorizer}
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.text.documentiterator.{LabelledDocument, SimpleLabelAwareIterator}
import org.deeplearning4j.text.sentenceiterator.{CollectionSentenceIterator, SentenceIterator}
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.indexing.BooleanIndexing
import org.nd4j.linalg.indexing.conditions.Conditions
import org.nd4j.linalg.ops.transforms.Transforms

import java.util

object VectorAnalysis {

  /** Creates TFIDF vectorizers for the usages passed as arguments.
   * Current implementation is designed to return Word2Vec representation models of the data.
   *
   * @param usages The collection of API usages consisting in different BoWs.
   *               Each of them with the characteristics of a Stack Overflow post.
   * @return A Map where the keys are the names of the models and the values correspond to the actual models.
   */
  def createTFIDFModel(usages: List[API_Usage_Text]): List[TfidfVectorizer] = {
    println("Fitting TF-IDF ...")
    // Converting BoWs to vectors - Training with all collected data
    val allTitles: List[List[String]] = usages.map(_.bows.head).filter(_.nonEmpty)
    val allTitlesModel: TfidfVectorizer = VectorAnalysis.tfidfModel(allTitles)

    val allQuestionBodies: List[List[String]] = usages.map(_.bows(1)).filter(_.nonEmpty)
    val allQuestionBodiesModel: TfidfVectorizer = VectorAnalysis.tfidfModel(allQuestionBodies)

    val allAnswerBodies: List[List[String]] = usages.map(_.bows(2)).filter(_.nonEmpty)
    val allAnswerBodiesModel: TfidfVectorizer = VectorAnalysis.tfidfModel(allAnswerBodies)

    List(allTitlesModel, allQuestionBodiesModel, allAnswerBodiesModel)
  }


  /** This method transforms a list of Bag of Words (passed as argument) to a list of vectors.
   * Several techniques for vector transformation from text could be adopted.
   * Word2Vec is adopted in this case for all words in the corpus.
   *
   * @param allDocuments The list of all documents to be transformed.
   * @return The vector representation of the documents.
   */
  def word2vecModel(allDocuments: List[List[String]]): Word2Vec = {
    val labelledDocuments = new util.ArrayList[LabelledDocument]()

    var index: Int = 0
    for (document <- allDocuments) {
      val labelledDocument: LabelledDocument = new LabelledDocument
      labelledDocument.addLabel(index.toString)
      labelledDocument.setContent(document.mkString(" "))

      labelledDocuments.add(labelledDocument)
      index += 1
    }
    val documentIterator = new SimpleLabelAwareIterator(labelledDocuments)

    val model: Word2Vec = new Word2Vec.Builder()
      .minWordFrequency(1)
      .iterations(10)
      .layerSize(100)
      .seed(42)
      .iterate(documentIterator)
      .build()

    model.fit()
    model
  }

  /** Given of mapping between words and their vector representation, transforms a document to
   * its vector representation.
   *
   * @param document A list of words to be transformed.
   * @param model A TF-IDF model able to transform the text passed as an argument into a single vector.
   * @return A single vector representing the document passed as an argument.
   */
  def document2Vector(document: List[String], model: TfidfVectorizer): Array[Double] = {
    val collectionWords: util.ArrayList[String] = new util.ArrayList[String]()
    document.foreach(word => collectionWords.add(word))

    if (collectionWords.isEmpty || (collectionWords.size() == 1 && collectionWords.get(0).trim.isEmpty))
      Array()
    else {
      val wordsTransformed: Array[Double] = model.transform(collectionWords).mean(0).toDoubleVector

      wordsTransformed
    }
  }

  /** This method transforms a list of Bag of Words (passed as argument) to a list of vectors.
   * Several techniques for vector transformation from text could be adopted.
   * TF-IDF is adopted in this case for all words in the corpus.
   *
   * @param allDocuments The list of all documents to be transformed.
   * @return The vector representation of the documents.
   */
  def tfidfModel(allDocuments: List[List[String]]): TfidfVectorizer = {
    val collectionSentences: util.ArrayList[String] = new util.ArrayList[String]()
    allDocuments.foreach(document => collectionSentences.add(document.mkString(" ")))
    val sentenceIterator: SentenceIterator = new CollectionSentenceIterator(collectionSentences)

    val vectorizer: TfidfVectorizer = new TfidfVectorizer.Builder()
      .setMinWordFrequency(1)
      .setIterator(sentenceIterator)
      .setTokenizerFactory(new DefaultTokenizerFactory)
      .build()

    vectorizer.fit()
    vectorizer
  }

  /** This method transforms a list of textual Bag of Words (passed as argument) to a list of vectors.
   *
   * @param allDocuments The list of all documents to be transformed.
   * @return The vector representation of the documents.
   */
  def bowModel(allDocuments: List[List[String]]): BagOfWordsVectorizer = {
    val collectionSentences: util.ArrayList[String] = new util.ArrayList[String]()
    allDocuments.foreach(document => collectionSentences.add(document.mkString(" ")))
    val sentenceIterator: SentenceIterator = new CollectionSentenceIterator(collectionSentences)

    val vectorizer: BagOfWordsVectorizer = new BagOfWordsVectorizer.Builder()
      .setMinWordFrequency(1)
      .setIterator(sentenceIterator)
      .setTokenizerFactory(new DefaultTokenizerFactory)
      .build()

    vectorizer.fit()
    vectorizer
  }

  /** Averages the vectors in an API usage
   *
   * @param usages The API vector usages
   * @return A list of API usages with their vectors combined
   */
  def averageCombination(usages: Array[API_Usage_Vector]): Array[API_Usage_Vector_Combined] = {
    usages.map(usage => {
      // In the case of TF-IDF vectors could have different sizes
      var vectors: Array[Array[Double]] = usage.vectors
      if (vectors.length == 1 && vectors.head.isEmpty)
        vectors = Array(Array.fill(100)(0.0))

      val allArrays: Array[Array[Double]] = vectors

      val allArraysStandard: Array[Array[Double]] = standardizeVectors(allArrays)
      val arrayVectors: INDArray = Nd4j.createFromArray(allArraysStandard)
      API_Usage_Vector_Combined(arrayVectors.mean(0).toDoubleVector)
    })
  }

  /** Transform all vectors to be of the same size
   *
   * @param matrix A matrix containing all vectors to be transformed
   * @return A standardize version of all vectors
   */
  def standardizeVectors(matrix: Array[Array[Double]]): Array[Array[Double]] = {
    // Standardizing all vectors to be the same size
    val maxSize: Int = matrix.map(_.length).max
    matrix.map(vector => {
      if (vector.length < maxSize) {
        val difference: Int = maxSize - vector.length
        vector.concat(Array.fill(difference)(0.0))
      } else vector
    })
  }

  /** Calculates the similarities between the vectors in specific combinations.
   *
   * @param combinations The array with the combinations made
   * @return A matrix with the values of similarities between the vectors in the combinations.
   */
  def similarityCombinations(combinations: Array[Array[Double]]): Array[Array[Double]] = {

    similarityMatrix(standardizeVectors(combinations))
  }

  /** Calculates similarities between the vectors passed as parameters.
   *
   * @param matrix The matrix containing the vectors from which similarities are going to be calculated.
   * @return Another matrix with the cosine similarities of all vectors with respect to each other.
   */
  def similarityMatrix(matrix: Array[Array[Double]]): Array[Array[Double]] = {
    var similarities: Array[Array[Double]] = Array()

    for(vector1 <- matrix) {
      val vector1Nd: INDArray = Nd4j.createFromArray(Array(vector1))
      var similaritiesVector1Nd: Array[Double] = Array()

      for(vector2 <- matrix) {
        val vector2Nd: INDArray = Nd4j.createFromArray(Array(vector2))
        val cosineSimilarityVectors: Double = Transforms.cosineSim(vector1Nd, vector2Nd)

        similaritiesVector1Nd :+= cosineSimilarityVectors
      }
      val similaritiesNd4j = Nd4j.createFromArray(Array(similaritiesVector1Nd))
      BooleanIndexing.replaceWhere(similaritiesNd4j, 0.0, Conditions.isNan)
      similarities :+= similaritiesNd4j.toDoubleVector
    }
    similarities
  }

  def jaccardSimilarity(callsList: List[API_Usage]): Array[Array[Double]] = {
    callsList.map(referenceAPIs => {
      callsList.map(accessAPIs => {
        val setReferences: Set[String] = referenceAPIs.bows.flatten.toSet
        val setAccess: Set[String] = accessAPIs.bows.flatten.toSet

        setReferences.intersect(setAccess).size.toDouble / setReferences.union(setAccess).size.toDouble
      }).toArray
    }).toArray
  }

}
