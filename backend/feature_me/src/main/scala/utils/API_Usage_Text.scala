package utils

/** Class that defines the main data structure to compare APIs given their text characteristics.
 * @param bows The list of Bag of Words
 */

class API_Usage_Text(override val bows: List[List[String]]) extends API_Usage(bows)
