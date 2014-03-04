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
import play.api.libs.functional.syntax._
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.json.Reads._
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.BSONObjectID.generate
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.JsValue
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import models.DocumentType
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import models.DocTypeProjectConnection
import scala.collection.mutable.{
  Map,
  SynchronizedMap,
  HashMap
}
import play.modules.reactivemongo.json.collection.JSONCollection

object DocumentTypeDao {

  /** The documenttype collection */
  def collection = db.collection[JSONCollection]("documenttypes")
  def stylesCollection = db.collection[JSONCollection]("docTypeStyles")
  val dbHelper = DBHelper

  def updateDocTypeStyle(query: JsObject, updateQuery: JsObject, upsert: Boolean = false) = {
    stylesCollection.update(query, updateQuery, GetLastError(), upsert)
      .map {
        lastError => lastError
      }
  }

  def createDocTypeStyle(docTypeStyle: JsObject): Future[Boolean] = {
    Logger.debug("Creating Doc Type Style: " + docTypeStyle)
    stylesCollection.insert(docTypeStyle).map {
      case ok if ok.ok => true
      case error => {
        Logger.error("Error creating document: " + docTypeStyle)
        Logger.error(error.err.getOrElse(""))
        false
      }
    }
  }

  def queryDocTypeStyles(query: JsObject, filter: Option[JsObject]): Future[List[JsObject]] = {
    try {
      val f = stylesCollection.find(query, filter).cursor[JsObject]
      f.collect[List](999).map {
        styles =>
          styles
      }
    } catch {
      case e: NoSuchElementException => Future(Nil) //Wrong ID
    }
  }

  def updateDocumentType(query: JsObject, updateQuery: JsObject, upsert: Boolean = false) = {
    collection.update(query, updateQuery, GetLastError(), upsert)
      .map {
        lastError => lastError
      }
  }

  /**
   * Returns the available (connected & active) DocTypes for a Project
   * id of docType
   * name of connection
   * description of connection
   * if of connection
   *
   */
  def getAvailableTypes(projectID: String): Future[List[DocTypeProjectConnection]] = {
    val query = Json.obj("connectedProjects." + projectID + ".active" -> true, "connectedProjects." + projectID -> Json.obj("$exists" -> true, "$not" -> Json.obj("$size" -> 0)))
    val filter = Json.obj("_id" -> 1, "connectedProjects." + projectID -> 1)

    val f = collection.find(query, filter).cursor[JsObject]
    val list = f.collect[List](10)
    val transformToArr = (__ \ "connectedProjects" \ projectID).json.pick

    list.map { resultList =>
      var result = List[DocTypeProjectConnection]()
      for (r <- resultList) {
        r.transform(transformToArr) match {
          case JsSuccess(value, path) => value.asOpt[List[DocTypeProjectConnection]] match {
            case Some(connections) => {
              Logger.debug("R is here! " + r.toString)
              for (con <- connections) {
                con.docTypeId = Some((r \ "_id" \ "$oid").asOpt[String].getOrElse(""))
                if (con.active)
                  result = result.+:(con)
              }
            }
          }
        }
      }
      result
    }
  }

  def getAvailableTypesNames(projectID: String): Future[Map[String, String]] = {
    val query = Json.obj("connectedProjects." + projectID + ".active" -> true, "connectedProjects." + projectID -> Json.obj("$exists" -> true, "$not" -> Json.obj("$size" -> 0)))
    val filter = Json.obj("_id" -> 1, "connectedProjects." + projectID -> 1)

    val f = collection.find(query, filter).cursor[JsObject]
    val list = f.collect[List](100)
    val transformToArr = (__ \ "connectedProjects" \ projectID).json.pick

    list.map { resultList =>
      val result = scala.collection.mutable.Map[String, String]()
      for (r <- resultList) {
        r.transform(transformToArr) match {
          case JsSuccess(value, path) => value.asOpt[List[DocTypeProjectConnection]] match {
            case Some(connections) => {
              for (con <- connections) {
                con.docTypeId = Some((r \ "_id" \ "$oid").asOpt[String].getOrElse(""))
                if (con.active)
                  result += (con._id.stringify -> con.name)
              }
            }
          }
        }
      }
      result
    }
  }

  def getAllTypesNames(projectID: String): Future[Map[String, String]] = {
    val query = Json.obj("connectedProjects." + projectID -> Json.obj("$exists" -> true, "$not" -> Json.obj("$size" -> 0)))
    val filter = Json.obj("_id" -> 1, "connectedProjects." + projectID -> 1)

    val f = collection.find(query, filter).cursor[JsObject]
    val list = f.collect[List](100)
    val transformToArr = (__ \ "connectedProjects" \ projectID).json.pick

    list.map { resultList =>
      val result = scala.collection.mutable.Map[String, String]()
      for (r <- resultList) {
        r.transform(transformToArr) match {
          case JsSuccess(value, path) => value.asOpt[List[DocTypeProjectConnection]] match {
            case Some(connections) => {
              for (con <- connections) {
                con.docTypeId = Some((r \ "_id" \ "$oid").asOpt[String].getOrElse(""))
                result += (con._id.stringify -> con.name)
              }
            }
          }
        }
      }
      result
    }
  }

  /*Logger.debug("CURRENTUSER: " + currentUser)
    val query = Json.obj("$or" -> Json.arr(
      Json.obj("author" -> currentUser),
      Json.obj("public" -> true)))
    val filter = Json.obj("_id" -> 1, "title" -> 1, "author" -> 1, "description" -> 1)

    val f = collection.find(query, filter)
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("modifiedAt" -> 1))
      .cursor[JsObject]

    f.collect[List](perPage).map {
      jRes => jRes
    }
  }
  * 
  */

  /**
   * Save a DocumentType.
   *
   * @return true, once saved.
   */
  def save(documentType: DocumentType): Future[DocumentType] = {
    Logger.debug("Saving Documenttype: " + documentType.name)
    println(Json.writes[DocumentType].writes(documentType))
    val timestamp: Long = System.currentTimeMillis()
    val insertDoc = documentType.copy(createdAt = timestamp, modifiedAt = timestamp)
    collection.insert(insertDoc).map {
      case ok if ok.ok =>
        insertDoc
      case error => throw new RuntimeException(error.message)
    }
  }

  def update(documentType: DocumentType): Future[Boolean] = {
    Logger.debug("Updating Documenttype: " + documentType.name)
    val timestamp: Long = System.currentTimeMillis()
    collection.save(documentType.copy(modifiedAt = timestamp)).map {
      case ok if ok.ok =>
        true
      case error => throw new RuntimeException(error.message)
    }
  }

  /**
   * The total number of DocumentTypes
   */
  def count: Future[Int] = {
    db.command(Count(collection.name))
  }

  def count(query: JsObject): Future[Int] = {
    // val x = BSONFormats.toBSON(query) match {
    //   case JsSuccess(value, path) => db.command(Count(collection.name, value))
    // }
    db.command(Count(collection.name, Some(BSONFormats.toBSON(query).get.asInstanceOf[BSONDocument])))
  }
  /**
   * Find all the documentTypes.
   *
   * @param page The page to retrieve, 0 based.
   * @param perPage The number of results per page.
   * @return All of the DocumentTypes.
   */
  def findAll(page: Int, perPage: Int): Future[Seq[DocumentType]] = {
    collection.find(Json.obj())
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("_id" -> -1))
      .cursor[DocumentType]
      .collect[List](perPage)
  }

  def findById(id: String, filter: Option[JsObject]): Future[Option[JsObject]] = {
    val query = dbHelper.toObjectId(id)
    try {
      filter match {
        case Some(filt) => {
          collection.find(query, filt).cursor[JsObject].collect[List](1).map {
            docTypes =>
              if (docTypes.isEmpty)
                None
              else
                Some(docTypes(0))
          }
        }
        case None => {

          collection.find(query).cursor[JsObject].collect[List](1).map {
            docTypes =>
              if (docTypes.isEmpty)
                None
              else
                Some(docTypes(0))
          }
        }
      }
    } catch {
      case e: Throwable => Future(None)
    }
  }

  def findByConnectionId(connectionID: String, projectID: String, filter: Option[JsObject]): Future[Option[JsObject]] = {
    //Json.obj("_id" -> Json.obj("$oid" -> id))
    val query = Json.obj("connectedProjects." + projectID + ".active" -> true, "connectedProjects." + projectID -> Json.obj("$elemMatch" -> dbHelper.toObjectId(connectionID)))
    try {
      filter match {
        case Some(filt) => {
          collection.find(query, filt).cursor[JsObject].collect[List](1).map {
            docTypes =>
              if (docTypes.isEmpty)
                None
              else
                Some(docTypes(0))
          }
        }
        case None => {
          collection.find(query).cursor[JsObject].collect[List](1).map {
            docTypes =>
              if (docTypes.isEmpty)
                None
              else
                Some(docTypes(0))
          }
        }
      }
    } catch {
      case e: Throwable => Future(None)
    }
  }

  def getAvailableTemplatesByProjectID(projectID: Option[String]): Future[Option[JsObject]] = {
    projectID match {
      case None => Future(None)
      case Some(projectID) =>
        val query = Json.obj("connectedProjects." + projectID + ".active" -> true, "connectedProjects." + projectID -> Json.obj("$exists" -> true, "$not" -> Json.obj("$size" -> 0)))
        val filter = Json.obj("_id" -> 1, "connectedProjects." + projectID -> 1, "listDesignTemplate" -> 1, "designTemplate" -> 1)
        val f = collection.find(query, filter).cursor[JsObject]
        val list = f.collect[List](999)
        val transformToArr = (__ \ "connectedProjects" \ projectID).json.pick
        list.map { resultList =>
          var result = Json.obj() //> res  : play.api.libs.json.JsObject = {}
          for (r <- resultList) {
            val connections = ((r \ "connectedProjects") \ projectID).asOpt[List[JsObject]].getOrElse(Nil)
            val designTemplate = (r \ "designTemplate").asOpt[String].getOrElse("")
            val listDesignTemplate = (r \ "listDesignTemplate").asOpt[String].getOrElse("")
            val docTypeID = ((r \ "_id") \ "$oid").asOpt[String].getOrElse("")
            for (con <- connections) {
              val conID = ((con \ "_id") \ "$oid").asOpt[String].getOrElse("")
              result = result + (conID, Json.obj("full" -> designTemplate, "list" -> listDesignTemplate, "docType" -> docTypeID))
            }
          }
          Some(result)
        }
    }
  }

  def getDocumentTypeLocales(filter: JsObject): Future[JsObject] = {
    //def getProjectTagsStruktur(projectID: String): Future[List[JsValue]] = {
    try {
      db.command(Aggregate("documenttypes", Seq(
        Match(BSONFormats.toBSON(filter).get.asInstanceOf[BSONDocument]),
        Unwind("elems"),
        Unwind("elems.locales"),
        Group(BSONDocument("_id" -> "$_id", "field" -> "$elems.fname", "lang" -> "$elems.locales.loc", "val" -> "$elems.locales.val"))(), //("count" -> SumValue(1)),
        Sort(Seq(Ascending("_id._id"), Ascending("_id._field")))))) map { stream =>

        (stream.toList.map { doc =>
          BSONFormats.toJSON(doc).asInstanceOf[JsObject]
        }).foldLeft(Json.obj())((acc, x) => {
          (((x \ "_id") \ "_id") \ "$oid").asOpt[String] match {
            case None => acc
            case Some(doctypeID) =>
              ((x \ "_id") \ "field").asOpt[String] match {
                case None => acc
                case Some(field) =>
                  ((x \ "_id") \ "lang").asOpt[String] match {
                    case None => acc
                    case Some(lang) =>
                      val res = ((x \ "_id") \ "val").asOpt[String].getOrElse("")
                      acc.transform(dbHelper.addLocale(doctypeID, lang, field, res)) match {
                        case JsSuccess(value, path) => value
                        case JsError(err) => acc
                      }
                  }
              }
          }
        })
      }
    } catch {
      case e: Throwable => null //Wrong ID
    }
  }
}