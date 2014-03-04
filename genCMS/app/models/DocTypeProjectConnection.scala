package models

import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectID.generate
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._

case class DocTypeProjectConnection(
  _id: BSONObjectID,
  name: String,
  description: String,
  active: Boolean,
  var docTypeId : Option[String]
  ) {
  /*
    
   override def toString() = {
    if (typ == "container" || typ == "row" || typ == "column") {
      "<div class=" + classes + ">" +  ""+ "</div>"
    } else if (typ == "element") {
      "<span class=" + classes + ">{{" + field + "}}</span>"
    } else if (typ == "label") {
      "<span class=" + classes + ">{{" + field + "}}</span>"
    } else {
      ""
    }
  }

* 
*/
}

object DocTypeProjectConnection {
  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val documenTypeConnFormat = Json.format[DocTypeProjectConnection]
}