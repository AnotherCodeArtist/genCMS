package service

import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import models.DocumentType
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import reactivemongo.api.collections.default.BSONCollection
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.bson.BSONDocument
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.modules.reactivemongo.json.BSONFormats
import play.api.libs.json.JsObject
import play.api.libs.json.JsObject

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
    f.collect[List](10)
  }

}