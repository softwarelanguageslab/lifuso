package utils

import java.io.{File, PrintWriter}

object FileOperations {

  /** Writes the similarity matrix passed as argument into a file.
   *
   * @param similarityMatrix The matrix of similarities (in case the name isn't very intuitive).
   * @param filePath The path of the file were the data is going to be saved.
   */
  def writeSimilaritiesTxt(similarityMatrix: Array[Array[Double]], filePath: String): Unit = {
    val pw: PrintWriter = new PrintWriter(new File(filePath))
    similarityMatrix.foreach(vector => {
      val vectorStr: String = vector.map(_.toString).mkString(",")
      pw.write(vectorStr + "\n")
    })
    pw.close()
  }

  /** Writes the usages of API passed as argument into a file.
   *
   * @param codeUsages The usages of APIs found on th code snippets.
   * @param filePath The path of the file were the data is going to be saved.
   */
  def writeCodeUsagesTxt(codeUsages: List[List[String]], filePath: String): Unit = {
    val pw: PrintWriter = new PrintWriter(new File(filePath))
    // Writing into a file only those API calls different
    codeUsages.foreach(usage => {
//      val usageStr: String = usage.map(usageDetails => s"${usageDetails._1}.${usageDetails._2.map(_.trim).mkString(".")}").distinct.mkString(",")
      pw.write(usage.mkString(",").trim + "\n")
    })
    pw.close()
  }

  /** Writes a bag of words per API usage corresponding to the processed textual information of the API.
   *
   * @param bows The bag of words to be written into a file.
   * @param filePath The path of the file were the data is going to be saved.
   */
  def writeNameCandidates(bows: Array[List[String]], filePath: String): Unit = {
    val pw: PrintWriter = new PrintWriter(new File(filePath))
    bows.foreach(bow => {
      val bowStr: String = bow.mkString(",")
      pw.write(bowStr + "\n")
    })
    pw.close()
  }

  /** Writes information related to the original title of the post and its link to Stack Overflow
   *
   * @param information The collection of titles and their links.
   * @param filePath The path of the file were the data is going to be saved.
   */
  def writeUserInformation(information: Array[(String, String)], filePath: String): Unit = {
    val pw: PrintWriter = new PrintWriter(new File(filePath))
    information.foreach(pair => {
      val pairStr: String = s"${pair._1}-->${pair._2}"
      pw.write(pairStr + "\n")
    })
    pw.close()
  }

  /** Writes the path of the libraries to automatically generates the paths
   *
   * @param groupID The groupID of the library.
   * @param artifactID The artifactID of the library.
   * @param filePath The path of the file were the data is going to be saved.
   */
  def writePath(groupID: String, artifactID: String, filePath: String): Unit = {
    val pw: PrintWriter = new PrintWriter(new File(filePath))
    pw.write(s"$groupID.$artifactID\n")
    pw.close()
  }
}

