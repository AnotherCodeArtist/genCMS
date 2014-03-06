package service

import scala.concurrent.Future
import scala.math.BigDecimal.double2bigDecimal

import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsArray
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads.BooleanReads
import play.api.libs.json.Reads.DoubleReads
import play.api.libs.json.Reads.IntReads
import play.api.libs.json.Reads.JsObjectReads
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.traversableReads
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.BSONFormats
import play.modules.reactivemongo.json.BSONFormats.toJSON
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.gridfs.GridFS
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.BSONArray
import reactivemongo.bson.BSONArrayIdentity
import reactivemongo.bson.BSONBooleanHandler
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentIdentity
import reactivemongo.bson.BSONDoubleHandler
import reactivemongo.bson.BSONIntegerHandler
import reactivemongo.bson.BSONStringHandler
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.bson.Producer.valueProducer
import reactivemongo.core.commands.Count
import reactivemongo.core.commands.GetLastError
import reactivemongo.core.commands.LastError
import reactivemongo.core.commands.RawCommand

object DocumentDao {

  /** The documenttype collection */
  def collection = db.collection[JSONCollection]("documents")
  collection.indexesManager.ensure(Index(Seq("loc" -> IndexType.Geo2D)))

  val gridFSImage = new GridFS(db, "images")
  val gridFSImageThumbs = new GridFS(db, "thumbs")
  val gridFSAudio = new GridFS(db, "audios")
  val gridFSVideo = new GridFS(db, "videos")

  val dbHelper = DBHelper

  /**
   * stores a new document in the db
   * @return true if saved, false if an error occured
   */
  def createDocument(document: JsObject): Future[Boolean] = {
    Logger.debug("Creating Document: " + document)
    collection.insert(document).map {
      case ok if ok.ok => true
      case error => {
        Logger.error("Error creating document: " + document)
        Logger.error(error.err.getOrElse(""))
        false
      }
    }
  }

  /**
   * The total number of Documents
   */
  def count: Future[Int] = {
    db.command(Count(collection.name))
  }

  def count(query: JsObject): Future[Int] = {
    db.command(Count(collection.name, Some(BSONFormats.toBSON(query).get.asInstanceOf[BSONDocument])))
  }

  def getDocumentById(id: String): Future[Option[JsObject]] = {
    try {
      val query = Json.obj("_id" -> Json.obj("$oid" -> id), "deleted" -> false)
      val f = collection.find[JsObject](query).cursor[JsObject]
      f.collect[List](1).map {
        documents =>
          if (documents.isEmpty)
            None
          else
            Some(documents(0))
      }
    } catch {
      case e: NoSuchElementException => Future(None) //Wrong ID
    }
  }

  def queryDocument(query: JsObject, filter: Option[JsObject]): Future[Option[JsObject]] = {
    try {
      val f = collection.find(query, filter).cursor[JsObject]
      f.collect[List](1).map {
        documents =>
          if (documents.isEmpty)
            None
          else
            Some(documents(0))
      }
    } catch {
      case e: NoSuchElementException => Future(None) //Wrong ID
    }
  }

  def getDocumentByIdAndFilter(id: String, filter: JsObject): Future[Option[JsObject]] = {
    try {
      id match {
        case "" => Future(None)
        case id =>
          val query = Json.obj("_id" -> Json.obj("$oid" -> id), "deleted" -> false)
          val f = collection.find(query, filter).cursor[JsObject]
          f.collect[List](1).map {
            documents =>
              if (documents.isEmpty)
                None
              else
                Some(documents(0))
          }
      }
    } catch {
      case e: NoSuchElementException => Future(None) //Wrong ID
    }
  }

  def connectTwoDocuments(id1: String, id2: String): Future[Boolean] = {
    if (id1 == "" && id2 == "") {
      Future(false)
    } else {
      val query = Json.obj("_id" -> Json.obj("$oid" -> id1))
      val updateQuery = Json.obj("$addToSet" -> Json.obj("connectedDocuments" -> id2))
      updateDocument(query, updateQuery).flatMap {
        case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
          val query = Json.obj("_id" -> Json.obj("$oid" -> id2))
          val updateQuery = Json.obj("$addToSet" -> Json.obj("connectedDocuments" -> id1))
          updateDocument(query, updateQuery).map {
            case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
              true
            case LastError(false, err, code, msg, _, _, _) =>
              false
          }
        case LastError(false, err, code, msg, _, _, _) =>
          Future(false)
      }
    }
    Future(false)
  }

  def getConnIDifUserAuthorized(documentID: String, user: GenUser): Future[Option[String]] = {
    try {
      val query = dbHelper.toObjectId(documentID)
      val filter = Json.obj("connection" -> 1, "fields.author" -> 1)

      val f = collection.find(query, filter).cursor[JsObject]
      f.collect[List](1).map {
        documents =>
          if (documents.isEmpty)
            None
          else {
            val projectID = (documents(0) \ "project").asOpt[String].getOrElse("")
            val author = ((documents(0) \ "fields") \ "author").asOpt[String].getOrElse("")
            Logger.debug("getConnIDifUserAuthorized: " + author + " " + user.identityId.userId)
            if (author == user.identityId.userId || user.isProjectAdmin(projectID)) {
              (documents(0) \ "connection").asOpt[String]
            } else { //user is not author and has got insufficient rights to save this document
              None
            }
          }
      }
    } catch {
      case e: NoSuchElementException => Future(None) //Wrong ID
    }
  }

  def updateDocument(query: JsObject, updateQuery: JsObject, upsert: Boolean = false) = {
    collection.update(query, updateQuery, GetLastError(), upsert)
      .map {
        lastError => lastError
      }
  }

  def getNearLocations(long: Double, lat: Double, maxDistance: Option[Double]): Future[Option[List[JsObject]]] = {
    var query: JsObject = null
    maxDistance match {
      case Some(maxDist) => query = Json.obj("loc" -> Json.obj("$near" -> Json.obj("$geometry" -> Json.obj("type" -> "Point", "coordinates" -> JsArray(Seq(JsNumber(long), JsNumber(lat)))), "$maxDistance" -> maxDist)))
      case _ => query = Json.obj("loc" -> Json.obj("$near" -> Json.obj("$geometry" -> Json.obj("type" -> "Point", "coordinates" -> JsArray(Seq(JsNumber(long), JsNumber(lat)))))))
    }
    try {
      val f = collection.find(query).cursor[JsObject]
      f.collect[List](100).map { documents =>
        if (documents.isEmpty)
          None
        else
          Some(documents)
      }
    } catch {
      case e: Throwable => Logger.error(e.getMessage()); Future(None)
    }
  }

  def getLocationsInBox(minLng: Double, minLat: Double, maxLng: Double, maxLat: Double): Future[Option[List[JsObject]]] = {
    val query = Json.obj("loc" -> Json.obj("$geoWithin" -> Json.obj("$box" -> List(List(minLng, minLat), List(maxLng, maxLat)))))
    val filter = Json.obj("loc" -> 1, "fields.geoloc" -> 1)
    try {
      val f = collection.find(query, filter).cursor[JsObject]
      f.collect[List](100).map { documents =>
        if (documents.isEmpty)
          None
        else
          Some(documents)
      }
    } catch {
      case e: Throwable => Logger.error(e.getMessage()); Future(None)
    }
  }

  def getNearLocationsWithDistance(long: Double, lat: Double, projectID: String): Future[Option[JsValue]] = {
    val command =
      BSONDocument(
        "geoNear" -> "documents",
        "near" -> BSONArray(long, lat),
        "spherical" -> true,
        "distanceMultiplier" -> 6371,
        "query" -> BSONDocument(
          "project" -> projectID))
    try {
      db.command(RawCommand(command)).map {
        result => Some(toJSON(result))
      }
    } catch {
      case e: Throwable => Logger.error(e.getMessage()); Future(None)
    }

  }

  /**
   * Validates the document Fields against the documenttype
   * returns None if no Validation errors are found
   * returns a List of errors if Validation errors are found
   * 	Error format: (FIELDNAME, ERROR)
   */
  def validateDocumentFields(document: JsObject)(implicit lang: play.api.i18n.Lang): Future[Option[List[(String, String)]]] = {
    val projectID = (document \ "project").as[String]
    val connectionID = (document \ "connection").as[String]
    val documentFields = (document \ "fields").as[JsObject]
    val filter = Json.obj("elems" -> 1)
    DocumentTypeDao.findByConnectionId(connectionID, projectID, Some(filter)).map {
      case None => Some(List(("", "DOCUMENT TYPE NOT FOUND!")))
      case Some(docType) =>
        (docType \ "elems").asOpt[List[JsObject]] match {
          case None => None //no elements in doc type -> validation OK
          case Some(elemList) =>
            var errors = List[(String, String)]()
            for (element <- elemList) {
              val fieldName = (element \ "fname").as[String]
              val required = (element \ "required").asOpt[Boolean].getOrElse(false)

              (element \ "type").asOpt[String] match {
                case Some("std") => None //omit value - can not be set by user!
                case Some("textLine" | "HTML") =>
                  val min = (element \ "min").asOpt[Int].getOrElse(-1)
                  val max = (element \ "max").asOpt[Int].getOrElse(-1)
                  val fieldValue = (documentFields \ fieldName).as[String]
                  //check min length
                  var error = List[String]()
                  if (fieldValue.length() < min) {
                    error = error ::: List(Messages("error.minLength", min))
                  }
                  //check max length
                  if (max != -1 && fieldValue.length() > max) {
                    error = error ::: List(Messages("error.maxLength", max))
                  }
                  //check reuired
                  if (required && fieldValue.length() == 0) {
                    error = error ::: List(Messages("error.required"))
                  }
                  error.isEmpty match {
                    case true =>
                    case false => errors = errors ::: List((fieldName, error.mkString("<br>")))
                  }
                case Some("img" | "audio" | "video") =>
                  val fieldValue = (documentFields \ fieldName).as[String]
                  if (required && fieldValue.length() == 0) {
                    errors = errors ::: List((fieldName, Messages("error.required")))
                  }
                case Some("boolean") =>
                  val fieldValue = (documentFields \ fieldName).asOpt[Boolean]
                  //Check type
                  fieldValue match {
                    case None =>
                      errors = errors ::: List((fieldName, Messages("error.expected.jsboolean")))
                    case _ => None
                  }
                case Some("number") =>
                  val fieldValue = (documentFields \ fieldName).asOpt[Double]
                  //Check type
                  fieldValue match {
                    case None =>
                      errors = errors ::: List((fieldName, Messages("error.expected.jsnumber")))
                    case _ => None
                  }
                case Some("geoLoc") =>

                  val lat = ((documentFields \ fieldName) \ "lat").asOpt[Double] match {
                    case None =>
                      ((documentFields \ fieldName) \ "lat").asOpt[String] match {
                        case None => None
                        case Some(str) => dbHelper.parseDouble(str)
                      }
                    case Some(lat) => Some(lat)
                  }
                  val lon = ((documentFields \ fieldName) \ "lon").asOpt[Double] match {
                    case None =>
                      ((documentFields \ fieldName) \ "lon").asOpt[String] match {
                        case None => None
                        case Some(str) => dbHelper.parseDouble(str)
                      }
                    case Some(lon) => Some(lon)
                  }
                  val display = ((documentFields \ fieldName) \ "display_name").asOpt[String]
                  //check required
                  if (required) {
                    if (!lat.isDefined || !lon.isDefined || !display.isDefined && display.getOrElse("").length == 0) {
                      errors = errors ::: List((fieldName, Messages("genCMS.docEdit.geoLoc.required")))
                    }
                  }
                case _ => None
              }
            }
            errors.isEmpty match {
              case true => None
              case false => Some(errors)
            }
        }
    }
  }

  def userIsAuthorOrAdmin(id: String, user: GenUser): Future[Option[String]] = {
    getDocumentByIdAndFilter(id, Json.obj("project" -> 1, "fields.author" -> 1)).map {
      case None => None //document not found
      case Some(document) =>
        val projectID = (document \ "project").asOpt[String].getOrElse("")
        user.isProjectAdmin(projectID) match {
          case true => Some("admin")
          case false =>
            if (user.identityId.userId == ((document \ "fields") \ "author").asOpt[String].getOrElse("")) {
              Some("author")
            } else {
              None
            }
        }
    }
  }

  def addTag(documentID: String, tag: String): Future[(Boolean, String)] = {
    val filter = Json.obj("tags" -> 1)
    getDocumentByIdAndFilter(documentID, filter).flatMap {
      case Some(document) =>
        (document \ "tags").asOpt[List[String]] match {
          case Some(tagList) =>
            tagList.indexOf(tag) match {
              case -1 => //add Tag to project
                val updateQuery = Json.obj("$set" -> Json.obj("tags" -> (tagList ::: List(tag)).sortWith(DBHelper.compAlphIgnoreCase)))
                updateDocument(dbHelper.toObjectId(documentID), updateQuery).map {
                  case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                    (true, "tag was added to project")
                  case LastError(false, err, code, msg, _, _, _) =>
                    (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
                }
              case _ => //already existing tag - no update needed
                Future(true, "tag already exists")
            }
          case None => //insert as new tag
            val updateQuery = Json.obj("$set" -> Json.obj("tags" -> List(tag)))
            updateDocument(dbHelper.toObjectId(documentID), updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                (true, "tag was added to project")
              case LastError(false, err, code, msg, _, _, _) =>
                (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
            }
        }
      case None => Future((false, "Document not found")) //update not possible
    }
  }

  def removeTag(documentID: String, tag: String): Future[(Boolean, String)] = {
    val filter = Json.obj("tags" -> 1)
    getDocumentByIdAndFilter(documentID, filter).flatMap {
      case Some(document) =>
        (document \ "tags").asOpt[List[String]] match {
          case Some(tagList) =>
            //check if tag exists and has to be removed
            tagList.indexOf(tag) match {
              case -1 =>
                Future(true, "Tag is already not present at document " + documentID)
              case index => //remove Tag
                val updateQuery = Json.obj("$set" -> Json.obj("tags" -> (DBHelper.removeAt(index, tagList)._1)))
                updateDocument(dbHelper.toObjectId(documentID), updateQuery).map {
                  case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                    (true, "tag was removed from project " + documentID)
                  case LastError(false, err, code, msg, _, _, _) =>
                    (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
                }
            }
          case None => Future(true, "Tag is already not present at document " + documentID)
        }
      case None => {
        Future(true, "Document not found" + documentID)
      }
    }
  }

}