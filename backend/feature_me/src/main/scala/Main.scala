import api_extraction.API_Extraction_Island
import dictionary.Dictionary
import utils.{API_Usage, API_Usage_Code, API_Usage_Text, API_Usage_Vector, API_Usage_Vector_Combined, FileOperations, Library, VectorAnalysis}
import org.apache.commons.csv.{CSVFormat, CSVParser}
import org.deeplearning4j.bagofwords.vectorizer.TfidfVectorizer
import text_analysis.{PerformTextAnalysis, PredefinedModels, TextAnalysis, TypedDependencies}

import java.io.{File, FileReader}
import scala.collection.mutable.{Map => MMap}
import scala.io.Source
import upickle.default._
import versions.ScrapVersions

object Main extends App {
  // Defining a library as example
  val libraryExample: Library = Library(groupID = "com.google.guava",
    artifactID = "guava")

  // Folder where the artifacts are going to be downloaded
  val DATA_FOLDER: String = "data/"

  // Obtaining the versions of a library
  val scrapper: ScrapVersions = new ScrapVersions(libraryExample)
  val libraryVersions: Library = scrapper.scrap()

  // Extracting information from the artifacts as a dictionary
  val dictionary: Dictionary = new Dictionary(library = libraryVersions, dataFolder = DATA_FOLDER)
  dictionary.createDictionary()

  // Reading dictionary
  val sourceDictionary: Source = Source.fromFile(s"$DATA_FOLDER/dictionaries/${libraryExample.groupID}" +
    s"|${libraryExample.artifactID}.json")
  val informationLibrary: String = sourceDictionary.getLines().mkString("")

  val dictionaryLibrary: MMap[String, Array[String]] = read[MMap[String, Array[String]]](informationLibrary)
  sourceDictionary.close()

  // Read CSV of posts from Stack Overflow (the approach for this has to be automatised)
  val csvReader = new FileReader(s"$DATA_FOLDER/so/${libraryExample.artifactID}.csv")
  val formatFile: CSVFormat = CSVFormat.DEFAULT.withHeader("Question_ID", "Title", "Tags", "Question_Body",
    "Question_CreationDate", "Question_LastEditDate", "Answer_ID", "Score", "Answer_Body",
    "Answer_CreationDate", "Answer_LastEditDate")

  val parser: CSVParser = CSVParser.parse(csvReader, formatFile)

  // Loading predefined models
  val (posTaggerModel, dictionaryLemmatizer) = PredefinedModels.lemmatization()

  // Form the corpus of words which is then going to be converted into vectors
  var apiUsagesText: Array[API_Usage_Text] = Array()
  var apiUsagesCode: Array[API_Usage_Code] = Array()

  // Make a bag of words per vector which will point out into the direction of a cluster name in
  // later phases of the processing
  var vectorBoW: Array[List[String]] = Array()
  var informationUser: Array[(String, String)] = Array()

  parser.forEach(record => {
    // Input for Code Extraction
    val answerBody: String = record.get("Answer_Body")
    val answerID: String = record.get("Answer_ID")

    // API Extraction from the Question Body
    // If there are usages matching the dictionary of public classes and methods, then
    // The text analysis will have to be performed, otherwise the answer should be excluded
    val codesAnswerBody: List[String] = API_Extraction_Island.extractCode(answerBody)

    if (codesAnswerBody.nonEmpty) { // There is a considerable number of answers with no code at all (e.g., for weka)
      // Filter out those fragments with less than 1 lines of code
      // which do not contain dots
      val linesGreater1Line: List[String] = codesAnswerBody.filter(_.split("\n").length > 1)
      val lines1Line: List[String] = codesAnswerBody.filterNot(_.split("\n").length > 1)
      val lines1LineDot: List[String] = lines1Line.filter(_.contains("."))

      val codeSelected: List[String] = lines1LineDot.concat(linesGreater1Line)
      val singleSnippet: String = codeSelected.mkString("\n")

      // Checking all types of code snippets with and without `imports`
      val usagesSnippet: List[String] = API_Extraction_Island.extractAPIs(singleSnippet)

      if (usagesSnippet.nonEmpty) { // If usages are extracted from the parser
        // Filtering usages extracted in the dictionary (only class names are checked here)
        val usagesLibrary: List[String] = usagesSnippet.map(_.trim).filter(usage => {
          val receiverName: String = usage.split("\\.").head
          dictionaryLibrary.keys.toList.contains(receiverName)
        })

        if (usagesLibrary.nonEmpty) {
          // Classes and methods
          val separation: List[(String, List[String])] = usagesLibrary.map(usage => {
            val dividedUsage: List[String] = usage.split("\\.").toList
            val methods: List[String] = dividedUsage.drop(1)

            (dividedUsage.head, methods)
          })

          // Input for Text Extraction
          val tags: String = record.get("Tags")
          val title: String = record.get("Title")
          val questionBody: String = record.get("Question_Body")

          // Text Extraction
//          // Tags
//          val tagsProcessed: List[String] = TextAnalysis.chunkTags(tags)

          // Title
          val titleProcessed: List[String] = TextAnalysis.filtering(
            PerformTextAnalysis.performTextAnalysis(title.split(" ").toList),
            libraryExample.artifactID
          )

          // Bodies
          val questionBodyProcessed: List[String] = TextAnalysis.filtering(
            PerformTextAnalysis.performTextAnalysis(TextAnalysis.extractBodyNoCode(questionBody)),
            libraryExample.artifactID
          )
          val answerBodyProcessed: List[String] =
            PerformTextAnalysis.performTextAnalysis(TextAnalysis.extractBodyNoCode(answerBody))

          // Class and method names
//          val rawClassNames: List[String] = TextAnalysis.lowering(separation.map(_._1))

          val rawMethodNames: List[String] = TextAnalysis.lowering(separation.flatMap(_._2))

//          val camelCaseClassNames: List[String] = TextAnalysis.lowering(
//            separation.map(usage => {
//              TextAnalysis.splitCamelCaseWord(usage._1).mkString(" ")
//            }).mkString(" ").split(" ").toList
//          )

          val camelCaseMethodNames: List[String] = TextAnalysis.lowering(
            separation.flatMap(usage => {
              usage._2.map(usage2 => TextAnalysis.splitCamelCaseWord(usage2).mkString(", "))
            })
          )

          // API Usages Text
          val apiUsageText: API_Usage_Text = new API_Usage_Text(List(titleProcessed, questionBodyProcessed,
            answerBodyProcessed))

          // API Usages Code
          val apiUsageCode: API_Usage_Code = new API_Usage_Code(List(rawMethodNames, camelCaseMethodNames, usagesLibrary))

          apiUsagesText :+= apiUsageText
          apiUsagesCode :+= apiUsageCode

          val relevantWordsTitle: List[String] = TypedDependencies.getRelevantWords(
            title, libraryExample.artifactID, isTitle = true)
          val relevantWordsQBody: List[String] = TypedDependencies.getRelevantWords(
            TextAnalysis.extractBodyNoCode(questionBody).mkString(" "), libraryExample.artifactID)
          val relevantWordsABody: List[String] = TypedDependencies.getRelevantWords(
            TextAnalysis.extractBodyNoCode(answerBody).mkString(" "), libraryExample.artifactID)

          val bows: List[String] = relevantWordsTitle.concat(relevantWordsQBody).concat(relevantWordsABody)
          informationUser :+= (title, s"https://stackoverflow.com/questions/$answerID")

          // If there are empty lines in the names, replaced them with <EMPTY>
          if (bows.isEmpty)
            vectorBoW :+= List("<EMPTY>")
          else
            vectorBoW :+= bows
        }
      }
    }
  })

  val usagesText: List[API_Usage_Text] = apiUsagesText.toList
  val usagesCode: List[API_Usage_Code] = apiUsagesCode.toList

  // Mapping of the characteristics and their corresponding vectors
  val models: List[TfidfVectorizer] = VectorAnalysis.createTFIDFModel(usagesText)

  // Applying the same process to different configurations
  def applyConfiguration(dataInformation: List[API_Usage],
                         modelSelection: List[TfidfVectorizer],
                         folderName: String,
                         fileName: String,
                         codeDimensions: Boolean = false): Unit = {
    println(s"Processing $folderName $fileName ...")
    var similarities: Array[Array[Double]] = Array()

    if (codeDimensions) {
      similarities = VectorAnalysis.jaccardSimilarity(dataInformation)
    } else {
      // Transforming the API usages into vectors
      val apiUsagesVectors: Array[API_Usage_Vector] = dataInformation.map(usage => {
        var i: Int = -1
        val vectorsGenerated: Array[Array[Double]] = usage.bows.map(bow => {
          i += 1
          VectorAnalysis.document2Vector(bow, modelSelection(i))
        }).toArray

        API_Usage_Vector(vectorsGenerated)
      }).toArray

      // Average the combination of vectors (currently all vectors are considered)
      val apiUsagesVectorsCombined: Array[API_Usage_Vector_Combined] = VectorAnalysis.averageCombination(apiUsagesVectors)

      // Cosine Similarity between combined vectors
      similarities = VectorAnalysis.similarityCombinations(apiUsagesVectorsCombined.map(_.combinedVector))
    }

    // Existence of folder to store combinations
    val combinationsFolder: String = s"$DATA_FOLDER/combinations/${libraryExample.artifactID}/$folderName/$fileName"
    val combinationsFolderFile: File = new File(combinationsFolder)

    if (!combinationsFolderFile.exists()) {
      combinationsFolderFile.mkdirs()
    }

    // Export as files to analyse in RStudio
    FileOperations.writeSimilaritiesTxt(similarities, s"$combinationsFolder/similarity_matrix.csv")
    FileOperations.writeCodeUsagesTxt(usagesCode.map(usage => usage.bows(2)),
      s"$combinationsFolder/code_usages.csv")
    FileOperations.writeNameCandidates(vectorBoW, s"$combinationsFolder/name_candidates.csv")
    FileOperations.writeUserInformation(informationUser, s"$combinationsFolder/extra_information.csv")

    println("Done!")
  }

  // Mixing combinations
  val textDimensions: List[String] = List("titles", "questionBodies", "answerBodies")
  val codeDimensions: List[String] = List("methods", "methodsCamelCase", "apiCalls")

  def generateCombinations(dimensions: List[String], usages: List[API_Usage], codeDimension: Boolean): Unit = {
    // Generate the index combinations
    for (index <- 1 to 3) {
      val combinations: List[List[String]] = dimensions.combinations(index).toList
      val combinationsIndex: List[List[Int]] = combinations.map(combination => {
        combination.map(element => dimensions.indexOf(element))
      })

      var p: Int = -1
      combinationsIndex.foreach(combination => {
        p += 1

      // For each element select only the indices in the combination
      val selectedUsages: List[API_Usage_Text] = usages.map(usage => {
        var k: Int = -1
        val selectedBows: List[List[String]] = usage.bows.filter(_ => {
            k += 1
            combination.contains(k)
          })
        new API_Usage_Text(selectedBows)
        })

        var k: Int = -1
        val selectedModels: List[TfidfVectorizer] = models.filter(_ => {
          k += 1
          combination.contains(k)
        })

        applyConfiguration(selectedUsages, selectedModels, index.toString, combinations(p).mkString("_"), codeDimension)
      })
    }
  }

  generateCombinations(codeDimensions, usagesCode, codeDimension = true)
  generateCombinations(textDimensions, usagesText, codeDimension = false)

 // Save the package path of the library in the analysis
 FileOperations.writePath(libraryExample.groupID, libraryExample.artifactID,
   s"$DATA_FOLDER/combinations/${libraryExample.artifactID}/path.txt")
}
