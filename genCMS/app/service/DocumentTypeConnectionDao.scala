package service

import scala.concurrent.Future

import play.api.Play.current
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.BSONFormats._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._

object DocumentTypeConnectionDao {
  def collection = db.collection[JSONCollection]("doctypeProjectConnection")
  val dbHelper = DBHelper
  /**
   * { 	"_id" : { "$oid" :" 234235" },
   * "project" : "_id" ... ,
   * "docType" : "_id" ... ,
   * "name" : "",
   * "description" :"",
   * "active" : T/F }
   */

  /**
   * Returns the available (connected & active) DocTypes for a Project
   * id of docType
   * name of connection
   * description of connection
   * if of connection
   *
   */
  def getAvailableTypes(projectID: String) = {
    val query = Json.obj("project._id" -> Json.obj("$oid" -> projectID))
    val filter = Json.obj("project" -> 0)
    val f = collection.find(query, filter).cursor[JsObject]
    f.collect[List](Integer.MAX_VALUE)
  }

}