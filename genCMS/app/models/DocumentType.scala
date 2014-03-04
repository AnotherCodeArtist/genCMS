package models

import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectID.generate
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._

case class DocumentType(
  author: String,
  name: String,
  linkedDocuments: List[BSONObjectID] = Nil,
  singleLineElems: List[SingleLineText] = Nil,
  htmlTextElems: List[HtmlText] = Nil,
  createdAt: Long=0,
  modifiedAt: Long=0,
  _id: BSONObjectID = BSONObjectID.generate)

trait DocumentElement {
  def name: String
  def ctype: String

}

case class SingleLineText(override val name: String, txt: String, override val ctype: String = "SingleLineText") extends DocumentElement
object SingleLineText {
  implicit val singleLineTextFormat = Json.format[SingleLineText]
}

case class HtmlText(override val name: String, html: String, override val ctype: String = "HtmlText") extends DocumentElement
object HtmlText {
  implicit val htmlTextFormat = Json.format[HtmlText]
}

object DocumentType {

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val documenTypeFormat = Json.format[DocumentType]
}