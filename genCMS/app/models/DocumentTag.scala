package models

import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectID.generate
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._
import scala.collection.mutable.Buffer
import play.api.libs.json.Writes
import play.api.libs.json.JsValue
import service.DBHelper



case class DocumentTag(
  name: String,
  order: Int,
  childs: Buffer[DocumentTag] = null) {
  
  
  def getChilds(): Option[List[String]] = {
    childs match {
      case null => None
      case childs =>
         
        Some((for (child <- childs.sortWith(DBHelper.compDocTag)) yield child.name).toList)
    }
  }
}

object DocumentTag {
  // Generates Writes and Reads for Feed and User thanks to Json Macros
  def compDocTag(e1: DocumentTag, e2: DocumentTag) = (e1.order < e2.order)
  
  implicit val writer = new Writes[DocumentTag] {
    def writes(c: DocumentTag): JsValue = {
      Json.obj("tag" -> c.name, "sort" -> c.order, "childs" -> c.getChilds)
    }
  }
}