package utils

case class Library(groupID: String, artifactID: String, versions: Option[List[String]] = None) {

  def addVersions(newVersions: List[String]): Library = {
    this.copy(versions = Some(newVersions))
  }
}
