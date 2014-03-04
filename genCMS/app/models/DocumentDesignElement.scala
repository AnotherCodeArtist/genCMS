package models

import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectID.generate
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._

case class DocumentDesignElement(
  typ: String,
  ftype: String,
  classes: String,
  field: String,
  childs: List[DocumentDesignElement] = Nil) {
  override def toString() = {
    if (typ == "container" || typ == "row" || typ == "column") {
      "<div class=\"" + classes + "\">" + childs.mkString + "</div>"
    } else if (typ == "element") {
      ftype match {
        case "std" => "<span class=\"" + classes + "\">{{{" + field + "}}}</span>"
        case "HTML" => "<span class=\"" + classes + "\">{{{" + field + "}}}</span>"
        case "img" => "<span class=\"" + classes + "\"><img class=\"hidden img-responsive\" type=\"genCMSimage\" src=\"img/{{{" + field + "}}}\" ></span>"
        case "audio" => "<span class=\"" + classes + "\"><div class=\"hidden\" type=\"audioplayer\">{{{" + field + "}}}</div></span>"
        case "video" => "<span class=\"" + classes + "\"><div class=\"hidden\" type=\"videoplayer\">{{{" + field + "}}}</div></span>"
        case "boolean" => "<span class=\"" + classes + "\">{{{" + field + "}}}</span>"
        case "number" => "<span class=\"" + classes + "\">{{{" + field + "}}}</span>"
        case "geoLoc" => "<span class=\"" + classes + "\"><div class=\"hidden\" type=\"mapcontent\">{{{" + field + "}}}</div></span>"
        case _ => "<span class=\"" + classes + "\">{{{" + field + "}}}</span>"
      }
    } else if (typ == "label") {
      "<span class=\"" + classes + "\"><label>{{{" + "genCMSlabel." + field + "}}}</label></span>"
    } else {
      ""
    }
  }
  def getUsedFields(): List[String] = {
    var field: List[String] = List()
    def getUsedFieldsRec(element: DocumentDesignElement): Unit = {
      if (element.typ == "element" || element.typ == "label") {
        println("add field " + element.field)
        field = field.+:(element.field)
      }
      if (!element.childs.isEmpty) {
        println("#### childs: " + element.childs.size)
        for (c <- element.childs) {
          println(c.typ)
          getUsedFieldsRec(c)
        }
      }
    }
    getUsedFieldsRec(this)
    println(field)
    field
  }
}

object DocumentDesignElement {
  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val documenTypeFormat = Json.format[DocumentDesignElement]
}