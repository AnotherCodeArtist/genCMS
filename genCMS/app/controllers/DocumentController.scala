package controllers

import play.api.mvc.Controller
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.api._
import reactivemongo.bson._
import play.modules.reactivemongo._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import reactivemongo.api.gridfs.Implicits.DefaultReadFileReader
import models.DocumentDesignElement
import service.DBHelper
import service.DocumentTypeDao
import service.EscapeHelper
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.BSONFormats
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import reactivemongo.api.gridfs.DefaultFileToSave
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import service.DocumentDao
import service.ProjectDao
import service.GenUser
import securesocial.core.SecureSocial

object DocumentController extends Controller with MongoController with SecureSocial {

  val docTypes = DocumentTypeDao.collection
  val docTypeDao = DocumentTypeDao
  val documentDao = DocumentDao
  val dbHelper = DBHelper

  //val gridFS = new GridFS(db, "attachements")

  val validateDocTypeConnection = (
    (__ \ 'name).json.copyFrom((__ \ 'name).json.pick[JsString]) and
    (__ \ 'description).json.copyFrom((__ \ 'description).json.pick[JsString]) and
    (__ \ 'active).json.copyFrom((__ \ 'active).json.pick[JsBoolean]) and
    (__ \ 'name).json.update(of[JsString].map { case JsString(str) => JsString(EscapeHelper.escapeHTML(str)) }) and
    (__ \ 'description).json.update(of[JsString].map { case JsString(str) => JsString(EscapeHelper.sanitizeHTML(str)) })) reduce

  def validateDocTypeConnectionUpdate(projectID: String) = ((
    (__ \ ("connectedProjects." + projectID + ".$.name")).json.copyFrom((__ \ 'name).json.pick[JsString]) and
    (__ \ ("connectedProjects." + projectID + ".$.description")).json.copyFrom((__ \ 'description).json.pick[JsString]) and
    (__ \ ("connectedProjects." + projectID + ".$.active")).json.copyFrom((__ \ 'active).json.pick[JsBoolean]) // and
    ) reduce) andThen ((
    (__ \ ("connectedProjects." + projectID + ".$.name")).json.update(of[JsString].map { case JsString(str) => JsString(EscapeHelper.escapeHTML(str)) }) and
    (__ \ ("connectedProjects." + projectID + ".$.description")).json.update(of[JsString].map { case JsString(str) => JsString(EscapeHelper.sanitizeHTML(str)) })) reduce)

  // "connectedProjects." + projectID+".$.name" -> "X")
  /*
  	     Expected JSON:
  	     { 	
  	     	"name": "...",
  	     	"description": "...",
  	     	"active": false
  	     }
  	     
  	     */

  val objectIdFormat = OFormat[String](
    (__ \ "$oid").read[String],
    OWrites[String] { s => Json.obj("$oid" -> s) })

  def createDocType = SecuredAction(ajaxCall = true, IsAdmin()).async(parse.json) { implicit request =>
    //TODO Validation
    //request.body.transform(validateDocType andThen addMongoIdAndDate).map { jsobj =>
    val currentUserEmail = request.user.identityId.userId
    request.body.transform(
      dbHelper.addCreationAndModificationDate andThen
        dbHelper.addCreateUser(currentUserEmail) andThen
        dbHelper.addChangeUser(currentUserEmail) andThen
        dbHelper.addMongoId).map { jsobj =>
        docTypes.insert(jsobj).map { p =>
          Ok(dbHelper.resOK(jsobj.transform(dbHelper.fromObjectId).get))
        }.recover {
          case e => InternalServerError(dbHelper.resKO(JsString("exception %s".format(e.getMessage))))
        }
      }.recoverTotal { err =>
        Future.successful(BadRequest(dbHelper.resKO(JsError.toFlatJson(err))))
      }
  }

  def getDocTypes(page: Int, perPage: Int, projectOnly: Boolean = false, userOnly: Boolean = false, orderBy: String = "_id", asc: Boolean = true) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    val projectId = request.session.get("project").getOrElse("")
    var query = Json.obj()
    if (projectOnly) {
      query = query ++ Json.obj("connectedProjects." + projectId -> Json.obj("$exists" -> true))
    }
    if (userOnly) {
      val userEmail = request.user.identityId.userId
      query = query ++ Json.obj("author" -> userEmail)
    }

    //val query = Json.obj("project.52d13e8c9d0000da016df9be" -> Json.obj("$exists" -> true))
    val filter = Json.obj("_id" -> 1, "author" -> 1, "name" -> 1, "connectedProjects." + projectId -> 1)

    val sort = Json.obj(orderBy -> (if (asc) 1 else -1))

    Logger.debug("Query: " + query.toString)
    Logger.debug("Filter: " + filter.toString)
    Logger.debug("sort: " + sort.toString)
    //count
    docTypeDao.count(query).flatMap { totalCount =>
      val f = docTypes.find(query, filter)
        .options(QueryOpts(skipN = page * perPage))
        .sort(sort)
        .cursor[JsValue]
      f.collect[List](perPage).map {
        jRes => Ok(dbHelper.resOK(Json.obj("results" -> totalCount) ++ Json.obj("doctypes" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectId)))
      }
    }
  }

  def getDocType(id: String) = Action.async { implicit request =>
    if (id == "0") {
      val newDocType = """{"name":"New Document Type", "author":"", "isPOI":false, 
    		  			"elems":[{"fname":"author","type":"std","locales":[{"loc":"de","val":"Autor"},{"loc":"en","val":"Author"}],"required":true,"sortOrder":1},
    		  				{"fname":"name","type":"std","locales":[{"loc":"de","val":"Name des Dokumenttyps"},{"loc":"en","val":"Documenttype Name"}],"required":true,"sortOrder":2},
							{"fname":"linkedDocuments","type":"std","locales":[{"loc":"de","val":"Verknüpfte Dokumente"},{"loc":"en","val":"Linked Documents"}],"required":false,"sortOrder":3},
							{"fname":"createdAt","type":"std","locales":[{"loc":"de","val":"Erstellungsdatum"},{"loc":"en","val":"Creation Date"}],"required":true,"sortOrder":4},
							{"fname":"modifiedAt","type":"std","locales":[{"loc":"de","val":"Änderungsdatum"},{"loc":"en","val":"Modification Date"}],"required":true,"sortOrder":5}]}"""
      Future(Ok(Json.parse(newDocType)))
    } else {
      docTypeDao.findById(id, None).map {
        res =>
          res match {
            case Some(jsobj) => Ok(jsobj)
            case None => Ok("")
          }
      }
    }
  }

  def copyDocType(id: String) = SecuredAction(ajaxCall = true, IsAdmin()).async(parse.empty) { implicit request =>
    Logger.debug("Copy DocType: " + id);
    val f = docTypes.find[JsValue](dbHelper.toObjectId(id)).cursor[JsValue]
    f.collect[List](1).flatMap {
      results =>
        if (!results.isEmpty) {
          val original = results(0)
          val nameAddition = " THIS IS A COPY"
          val currentUserEmail = request.user.identityId.userId
          val time = (new java.util.Date).getTime()
          original.transform(dbHelper.transformToDocTypeCopy(nameAddition, currentUserEmail, time)) match {
            case JsSuccess(jsobj, path) => {

              Logger.error("modified: " + jsobj.toString)
              docTypes.insert(jsobj).map { p =>
                Ok(dbHelper.resOK(jsobj.transform(dbHelper.fromObjectId).get))
              }.recover {
                case e => Ok(dbHelper.resKO(JsString("exception %s".format(e.getMessage))))
              }
            }
            case JsError(errors) => Future(Ok(dbHelper.resKO(Json.obj("error" -> errors.toString))))
          }
        } else {
          Future.successful(Ok(""))
        }
    }
  }

  def test() = Action.async(parse.json) { implicit request =>
    Logger.debug("TEST")
    Logger.debug(request.body.toString)
    val time = (new java.util.Date).getTime()
    Logger.debug("Time: " + time)
    request.body.validate(__.json.update(dbHelper.generateCreated(time))).map {
      json =>
        {
          Logger.debug(json.toString)
        }
    }
    Future(Ok(""))
  }

  def updateDocType(id: String) = SecuredAction(ajaxCall = true, IsAdmin()).async(parse.json) { implicit request =>
    //request.body.transform(validateDocType).flatMap { jsobj =>
    //TODO Validation
    val currentUserEmail = request.user.identityId.userId
    request.body.transform(dbHelper.addChangeUser(currentUserEmail)).map { jsObj =>
      docTypes.save(jsObj).map { lastError =>
        if (lastError.ok)
          Ok(dbHelper.resOK(Json.obj("msg" -> s"document type $id updated")))
        else
          InternalServerError(dbHelper.resKO(JsString("error %s".format(lastError.stringify))))
      }
    }.recoverTotal { err =>
      Future.successful(BadRequest(dbHelper.resKO(JsError.toFlatJson(err))))
    }
  }

  def connectDocTypeToProject(docTypeId: String) = SecuredAction(ajaxCall = true, IsAdmin()).async(parse.json) { implicit request =>
    //check if an id is set -> update or create
    request.session.get("project") match {
      case Some(projectID) => {
        request.body.transform((__ \ '_id \ '$oid).json.pick[JsString]) match {
          case JsSuccess(connectionID, path) => { //update
            request.body.transform(validateDocTypeConnectionUpdate(projectID)) match {
              case JsSuccess(setquery, path) => {
                Logger.debug("Query: " + setquery)

                val updateQuery = Json.obj("$set" -> setquery)
                //Json.obj("connectedProjects." + projectID+".$.name" -> "CENIS"))

                Logger.debug("UpdateQuery: " + updateQuery)
                val searcQuery = Json.obj("connectedProjects." + projectID + "._id" -> Json.obj("$oid" -> connectionID))
                docTypeDao.updateDocumentType(searcQuery, updateQuery).map {
                  case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                    val result = Json.obj(
                      "result" -> JsString("OK"),
                      "doc" -> BSONFormats.toJSON(doc),
                      "updated" -> updated,
                      "updatedExisting" -> updatedExisting)
                    updated match {
                      case 1 => Ok(dbHelper.resOK(result))
                      case _ => Ok(dbHelper.resKO(result))
                    }

                  }
                  case LastError(false, err, code, msg, _, _, _) => {
                    val error = Json.obj(
                      "error" -> JsString(err.getOrElse("unknown")),
                      "code" -> JsNumber(code.getOrElse[Int](0)),
                      "msg" -> JsString(msg.getOrElse("no messsage")))
                    Ok(dbHelper.resKO(error))
                  }
                }
              }
              case JsError(errors) => {
                Logger.debug(dbHelper.resKO(Json.obj("error" -> errors.toString)).toString)
                Future(Ok(dbHelper.resKO(Json.obj("error" -> errors.toString))))
              }

            }
          }
          case JsError(errors) => { //Insert
            request.body.transform(validateDocTypeConnection andThen dbHelper.addMongoId) match {
              case JsSuccess(query, path) => {
                Logger.debug("Query: " + query)

                val updateQuery = Json.obj("$push" -> Json.obj("connectedProjects." + projectID -> query))

                Logger.debug("UpdateQuery: " + updateQuery)
                docTypeDao.updateDocumentType(dbHelper.toObjectId(docTypeId), updateQuery).map {
                  case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                    val result = Json.obj(
                      "result" -> JsString("OK"),
                      "doc" -> BSONFormats.toJSON(doc),
                      "updated" -> updated,
                      "updatedExisting" -> updatedExisting)
                    updated match {
                      case 1 => {
                        Ok(dbHelper.resOK(updateQuery \ "$push" \ ("connectedProjects." + projectID)))
                      }
                      case _ => Ok(dbHelper.resKO(result))
                    }

                  }
                  case LastError(false, err, code, msg, _, _, _) => {
                    val error = Json.obj(
                      "error" -> JsString(err.getOrElse("unknown")),
                      "code" -> JsNumber(code.getOrElse[Int](0)),
                      "msg" -> JsString(msg.getOrElse("no messsage")))
                    Ok(dbHelper.resKO(error))
                  }
                }
              }
              case JsError(errors) => {
                Logger.debug(dbHelper.resKO(Json.obj("error" -> errors.toString)).toString)
                Future(Ok(dbHelper.resKO(Json.obj("error" -> errors.toString))))
              }
            }
          }
        }
      }
      case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "No Project Selected!"))))
    }
  }

  def disconnectDocTypeFromProject(docTypeID: String, connectionID: String) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    request.session.get("project") match {
      case Some(projectID) => {
        //TODO
        val updateQuery = Json.obj("$pull" -> Json.obj("connectedProjects." + projectID -> dbHelper.toObjectId(connectionID)))
        Logger.debug("UpdateQuery: " + updateQuery)
        docTypeDao.updateDocumentType(dbHelper.toObjectId(docTypeID), updateQuery).map {
          case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
            val result = Json.obj(
              "result" -> JsString("OK"),
              "doc" -> BSONFormats.toJSON(doc),
              "updated" -> updated,
              "updatedExisting" -> updatedExisting)
            updated match {
              case 1 => Ok(dbHelper.resOK(result))
              case _ => Ok(dbHelper.resKO(result))
            }

          }
          case LastError(false, err, code, msg, _, _, _) => {
            val error = Json.obj(
              "error" -> JsString(err.getOrElse("unknown")),
              "code" -> JsNumber(code.getOrElse[Int](0)),
              "msg" -> JsString(msg.getOrElse("no messsage")))
            Ok(dbHelper.resKO(error))
          }
        }
        //Validate request.body
        //Update Project
      }
      case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "No Project Selected!"))))
    }
  }

  def getConnectedDocumentTypes() = SecuredAction(ajaxCall = true).async { implicit request =>
    Logger.debug("getConnected")
    request.session.get("project") match {
      case Some(projectID) => {
        docTypeDao.getAvailableTypes(projectID).map { result =>
          {
            Logger.debug(result.toString)
            Ok(dbHelper.resOK(Json.toJson(result))) //c.mkString(" | "))
          }
        }
      }
      case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "No Project Selected!"))))
    }
  }

  def deleteDocType(id: String) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    docTypes.remove(dbHelper.toObjectId(id)).map {
      case ok if ok.ok => Ok(dbHelper.resOK(Json.obj("msg" -> s"document type $id deleted")))
      case error => InternalServerError(dbHelper.resKO(JsString("error %s".format(error.stringify))))
    }
  }

  def saveDocTypeDesign(id: String) = SecuredAction(ajaxCall = true, IsAdmin()).async(parse.json) { implicit request =>
    //TODO Validation
    //TODO Generate HTML Template with 
    val d = (request.body).validate[DocumentDesignElement]
    var res: DocumentDesignElement = null
    d.map { d: DocumentDesignElement => res = d }
    val template = res.toString
    val filter = res.getUsedFields
    Logger debug ("filter created")
    Logger.error(filter toString)
    val modifier = Json.obj("$set" -> Json.obj(
      "design" -> request.body,
      "designTemplate" -> template,
      "designFilter" -> filter))
    docTypes.update(dbHelper.toObjectId(id), modifier).map {
      _ => Ok("")
    }
  }

  def getDocTypeDesign(id: String) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    val query = dbHelper.toObjectId(id)
    val filter = Json.obj("_id" -> 1, "design" -> 1)

    val f = docTypes.find(query, filter).cursor[JsValue]
    f.collect[List](1).map {
      docTypes =>
        if (docTypes.isEmpty)
          Ok("")
        else
          Ok(Json.toJson(docTypes(0)))
    }
  }

  def saveDocTypeListDesign(id: String) = SecuredAction(ajaxCall = true, IsAdmin()).async(parse.json) { implicit request =>
    val d = (request.body).validate[DocumentDesignElement]
    var res: DocumentDesignElement = null
    d.map { d: DocumentDesignElement => res = d }
    val template = res.toString
    val filter = res.getUsedFields
    Logger debug ("filter created")
    Logger.error(filter toString)
    val modifier = Json.obj("$set" -> Json.obj(
      "listDesign" -> request.body,
      "listDesignTemplate" -> template,
      "listDesignFilter" -> filter))
    docTypes.update(dbHelper.toObjectId(id), modifier).map {
      _ => Ok("")
    }
  }

  def getDocTypeListDesign(id: String) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    val query = dbHelper.toObjectId(id)
    val filter = Json.obj("_id" -> 1, "listDesign" -> 1)

    val f = docTypes.find(query, filter).cursor[JsValue]
    f.collect[List](1).map {
      docTypes =>
        if (docTypes.isEmpty)
          Ok("")
        else
          Ok(Json.toJson(docTypes(0)))
    }
  }

  /**
   * Create a new Document according to supplied DocType and connectionID
   */
  def createDocument(connectionID: String, connectToDoc: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    val userEmail = request.user.identityId.userId
    request.session.get("project") match {
      case Some(projectID) => {
        request.user.asInstanceOf[GenUser].isProjectAuthor(projectID) match {
          case true => //allowed to create documents
            documentDao.getDocumentByIdAndFilter(connectToDoc, Json.obj("_id" -> 1)).flatMap { existingDoc =>
              if (connectToDoc != "" && !existingDoc.isDefined) {
                Future(Ok(dbHelper.resKO(Json.obj("error" -> "No Document to connect to"))))
              } else {
                docTypeDao.findByConnectionId(connectionID, projectID, None).flatMap { res =>
                  res match {
                    case Some(docType) => {
                      Logger.debug(docType.toString)
                      val elements = Json.toJson((docType \ "elems").as[List[JsObject]])
                      val template = (docType \ "designTemplate").asOpt[String]
                      val listTemplate = (docType \ "listDesignTemplate").asOpt[String]
                      val document = Json.obj("fields" -> dbHelper.getJsonObject((for (s <- (docType \ "elems").as[List[JsObject]]) yield (s \ "type").as[String] match {
                        case "geoLoc" => Json.obj((s \ "fname").as[String] -> Json.obj(
                          "lon" -> JsNull,
                          "lat" -> JsNull,
                          "display_name" -> JsNull,
                          "address" -> JsNull))

                        /*
                     *  "address" -> Json.obj(
                    "house_number" -> JsNull, "road" -> JsNull, "city" -> JsNull, "city_district" -> JsNull, "state" -> JsNull, "county" -> JsNull, "postcode" -> JsNull, "country" -> JsNull, "country_code" -> JsNull)))
                     */
                        case "number" => Json.obj((s \ "fname").as[String] -> 0)
                        case "boolean" => Json.obj((s \ "fname").as[String] -> false)
                        case _ => Json.obj((s \ "fname").as[String] -> "")
                      })))
                      val doc = document.transform(dbHelper.addCreationAndModificationDate)
                      document.transform(
                        dbHelper.addTitle("Document " + (new java.util.Date).getTime()) andThen
                          dbHelper.addDeleted(false) andThen
                          dbHelper.addPublished(false) andThen
                          dbHelper.addInEdit(true) andThen
                          dbHelper.addReported(false) andThen
                          dbHelper.addTags() andThen
                          dbHelper.addLoc() andThen
                          dbHelper.addDocumentCreationAndModificationDate andThen
                          dbHelper.addDocumentCreateUser(userEmail) andThen
                          dbHelper.addChangeUser(userEmail) andThen
                          dbHelper.addProjectID(projectID) andThen
                          dbHelper.addConnectionID(connectionID) andThen
                          dbHelper.addMongoId) match {
                          case JsSuccess(doc, path) => {
                            documentDao.createDocument(doc).map { p =>
                              p match {
                                case true =>
                                  val newID = ((doc \ "_id") \ "$oid").asOpt[String].getOrElse("")
                                  if (connectToDoc != "" && existingDoc.isDefined) {
                                    documentDao.connectTwoDocuments(newID, connectToDoc);
                                  }
                                  Ok(dbHelper.resOK(
                                    Json.obj("document" -> Json.toJson(doc)) ++
                                      Json.obj("elements" -> Json.toJson(elements)) ++
                                      Json.obj("template" -> template) ++
                                      Json.obj("listTemplate" -> listTemplate)))
                                case false => Ok(dbHelper.resKO(Json.obj("error" -> "Document could not be created")))
                              }
                            }
                          }
                          case JsError(error) => Future(Ok(dbHelper.resKO(Json.obj("error" -> error.toString))))
                        }
                      //Ok(dbHelper.resOK(Json.toJson(docType))) //c.mkString(" | "))
                    }
                    case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "Document Type does not exist"))))
                  }
                }
              }
            }
          case false => //not allowed to create documents
            //{"error":"Not authorized"}
            Future(Forbidden(Json.obj("error" -> "Not authorized")))
        }

      }
      case None => {
        Future(Ok(dbHelper.resKO(Json.obj("error" -> "No Project Selected!"))))
      }
    }
  }

  def getMyDocuments(page: Int, perPage: Int, projectOnly: Boolean = true, filteredOnly: Boolean = false, inEdit: Boolean = true, published: Boolean = false, deleted: Boolean = false, orderBy: String = "_id", asc: Boolean = true) = SecuredAction(ajaxCall = true).async { implicit request =>
    val userEmail = request.user.identityId.userId
    val projectId = request.session.get("project").getOrElse("")

    var query = Json.obj("fields.author" -> userEmail, "deleted" -> false)
    if (projectOnly) {
      query = query ++ Json.obj("project" -> projectId)
    }
    if (filteredOnly) { //Apply filters (edit, published) only if checked
      query = query ++ Json.obj("inEdit" -> inEdit, "published" -> published)
    }

    val filter = Json.obj("_id" -> 1, "changeAuthor" -> 1, "connection" -> 1, "deleted" -> 1, "inEdit" -> 1, "project" -> 1, "published" -> 1, "title" -> 1, "fields.modifiedAt" -> 1, "fields.createdAt" -> 1)

    val sort = Json.obj(orderBy -> (if (asc) 1 else -1))

    documentDao.count(query).flatMap { totalCount =>
      val f = documentDao.collection.find(query, filter)
        .options(QueryOpts(skipN = page * perPage))
        .sort(sort)
        .cursor[JsValue]
      f.collect[List](perPage).map {
        jRes => Ok(dbHelper.resOK(Json.obj("results" -> totalCount) ++ Json.obj("documents" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectId)))
      }
    }
    //Future(Ok(""))
  }

  def getUnreleasedDocuments(page: Int, perPage: Int, orderBy: String = "_id", asc: Boolean = true) = SecuredAction(ajaxCall = true).async { implicit request =>
    val userEmail = request.user.identityId.userId
    val projectId = request.session.get("project").getOrElse("")

    val query = Json.obj("deleted" -> false, "inEdit" -> false, "published" -> false, "project" -> projectId)

    val filter = Json.obj("_id" -> 1, "changeAuthor" -> 1, "connection" -> 1, "deleted" -> 1, "inEdit" -> 1, "project" -> 1, "published" -> 1, "title" -> 1, "fields.modifiedAt" -> 1, "fields.createdAt" -> 1)

    val sort = Json.obj(orderBy -> (if (asc) 1 else -1))

    documentDao.count(query).flatMap { totalCount =>
      val f = documentDao.collection.find(query, filter)
        .options(QueryOpts(skipN = page * perPage))
        .sort(sort)
        .cursor[JsValue]
      f.collect[List](perPage).map {
        jRes => Ok(dbHelper.resOK(Json.obj("results" -> totalCount) ++ Json.obj("documents" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectId)))
      }
    }
  }

  def getAllDocuments(page: Int, perPage: Int, projectOnly: Boolean = true, filteredOnly: Boolean = false, inEdit: Boolean = true, published: Boolean = false, deleted: Boolean = false, orderBy: String = "_id", asc: Boolean = true) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    val userEmail = request.user.identityId.userId
    val projectId = request.session.get("project").getOrElse("")
    var query = Json.obj("fields.author" -> userEmail, "deleted" -> false)
    if (projectOnly) {
      query = query ++ Json.obj("project" -> projectId)
    }
    if (filteredOnly) { //Apply filters (edit, published) only if checked
      query = query ++ Json.obj("inEdit" -> inEdit, "published" -> published)
    }

    val filter = Json.obj("_id" -> 1, "changeAuthor" -> 1, "connection" -> 1, "deleted" -> 1, "inEdit" -> 1, "project" -> 1, "published" -> 1, "title" -> 1, "fields.modifiedAt" -> 1, "fields.createdAt" -> 1)

    val sort = Json.obj(orderBy -> (if (asc) 1 else -1))

    documentDao.count(query).flatMap { totalCount =>
      val f = documentDao.collection.find(query, filter)
        .options(QueryOpts(skipN = page * perPage))
        .sort(sort)
        .cursor[JsValue]
      f.collect[List](perPage).map {
        jRes => Ok(dbHelper.resOK(Json.obj("results" -> totalCount) ++ Json.obj("documents" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectId)))
      }
    }
    //Future(Ok(""))
  }

  /* def getDocTypes(page: Int, perPage: Int, projectOnly: Boolean = false, userOnly: Boolean = false, orderBy: String = "_id", asc: Boolean = true) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    val projectId = request.session.get("project").getOrElse("")
    var query = Json.obj()
    if (projectOnly) {
      query = query ++ Json.obj("connectedProjects." + projectId -> Json.obj("$exists" -> true))
    }
    if (userOnly) {
      val userEmail = request.user.identityId.userId
      query = query ++ Json.obj("author" -> userEmail)
    }

    //val query = Json.obj("project.52d13e8c9d0000da016df9be" -> Json.obj("$exists" -> true))
    val filter = Json.obj("_id" -> 1, "author" -> 1, "name" -> 1, "connectedProjects." + projectId -> 1)

    val sort = Json.obj(orderBy -> (if (asc) 1 else -1))

    Logger.debug("Query: " + query.toString)
    Logger.debug("Filter: " + filter.toString)
    Logger.debug("sort: " + sort.toString)
    //count
    docTypeDao.count(query).flatMap { totalCount =>
      val f = docTypes.find(query, filter)
        .options(QueryOpts(skipN = page * perPage))
        .sort(sort)
        .cursor[JsValue]
      f.collect[List](perPage).map {
        jRes => Ok(dbHelper.resOK(Json.obj("results" -> totalCount) ++ Json.obj("doctypes" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectId)))
      }
    }
  }
*/
  def getDocument(documentID: String) = Action.async { implicit request =>
    val projectID = Application.getSessionValue(request, "project")
    documentDao.getDocumentById(documentID).flatMap { res =>
      res match {
        case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "No Such Document"))))
        case Some(document) => {
          (document \ "connection").asOpt[String] match {
            case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "Document is not connected to any Document Type"))))
            case Some(connectionID) => {
              docTypeDao.findByConnectionId(connectionID, projectID, None).map { res =>
                res match {
                  case None => Ok(dbHelper.resKO(Json.obj("error" -> "Document Type could not be found")))
                  case Some(docType) => {
                    val elements = Json.toJson((docType \ "elems").as[List[JsObject]])
                    val template = (docType \ "designTemplate").asOpt[String]
                    val listTemplate = (docType \ "listDesignTemplate").asOpt[String]
                    Ok(dbHelper.resOK(
                      Json.obj("document" -> Json.toJson(document)) ++
                        Json.obj("elements" -> Json.toJson(elements)) ++
                        Json.obj("template" -> template) ++
                        Json.obj("listTemplate" -> listTemplate)))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /*
   * request.session.get("project") match {
      case Some(projectID) =>
        val connectionQueryExt = (request.body \ "connectionID").asOpt[String] match {
          case Some("") => Json.obj()
          case Some(connectionID) => Json.obj("connection" -> connectionID)
          case None => Json.obj()
        }
        val tagsQueryExt = (request.body \ "tags").asOpt[List[String]] match {
          case Some(list) =>
            if (list.isEmpty)
              Json.obj()
            else
              Json.obj("tags" -> Json.obj("$all" -> list))
          case None => Json.obj()
        }
        val documentQuery = Json.obj("project" -> projectID, "deleted" -> false, "published" -> true) ++ connectionQueryExt ++ tagsQueryExt
        Logger.debug("DOC QUERY Connections: " + documentQuery);
        projectDao.getSelectedProjectConnectionsWithCount(documentQuery).flatMap { connRes =>
          DocumentTypeDao.getAvailableTypesNames(projectID).map { docTypeResMap =>
            val result = for (conn <- connRes) yield {
              val id = (conn \ "_id").asOpt[String].getOrElse("")
              conn ++ Json.obj("name" -> (docTypeResMap.getOrElse(id, "").asInstanceOf[String]))
            }
            Ok(dbHelper.resOK(Json.obj("connections" -> result)))
          }

        }
      case None => Future(Ok(dbHelper.resKO(Json.toJson("no project selected"))))
    }
   */

  def getDocumentsList(page: Int, perPage: Int) = SecuredAction(ajaxCall = true).async(parse.json) { implicit request =>
    //, filteredOnly:Boolean, connectionID: String, tag: String, orderBy: String, asc: Boolean
    //  val projectId = request.session.get("project").getOrElse("")
    request.session.get("project") match {
      case Some(projectID) =>
        val userEmail = request.user.identityId.userId
        val orderBy = (request.body \ "orderBy").asOpt[String].getOrElse("_id")
        val asc = (request.body \ "asc").asOpt[Boolean].getOrElse(false)
        val connectionQueryExt = (request.body \ "connectionID").asOpt[String] match {
          case Some("") => Json.obj()
          case Some(connectionID) => Json.obj("connection" -> connectionID)
          case None => Json.obj()
        }
        val connectedDocQueryExt = (request.body \ "connectedToDoc").asOpt[String] match {
          case None => Json.obj()
          case Some("") => Json.obj()
          case Some(id) => Json.obj("connectedDocuments" -> Json.obj("$all" -> List(id)))
        }
        val tagsQueryExt = (request.body \ "tags").asOpt[List[String]] match {
          case Some(list) =>
            if (list.isEmpty)
              Json.obj()
            else
              Json.obj("tags" -> Json.obj("$all" -> list))
          case None => Json.obj()
        }

        val query = Json.obj("deleted" -> false, "project" -> projectID, "published" -> true) ++ connectionQueryExt ++ tagsQueryExt ++ connectedDocQueryExt
        //	TODO FIlter merger from all connections?!
        val filter = Json.obj("connection" -> 1, "fields" -> 1) //Json.obj("_id" -> 1, "connection" -> 1, "deleted" -> 1, "inEdit" -> 1, "project" -> 1, "published" -> 1, "title" -> 1, "fields.modifiedAt" -> 1, "fields.createdAt" -> 1)
        val sort = Json.obj(orderBy -> (if (asc) 1 else -1))

        documentDao.count(query).flatMap { totalCount =>
          val f = documentDao.collection.find(query, filter)
            .options(QueryOpts(skipN = page * perPage))
            .sort(sort)
            .cursor[JsValue]
          f.collect[List](perPage).map {
            jRes => Ok(dbHelper.resOK(Json.obj("results" -> totalCount) ++ Json.obj("documents" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectID)))
          }
        }

      case None => Future(Ok(dbHelper.resKO(Json.toJson("no project selected"))))
    }
  }

  def getDocumentDetailView(id: String) = Action.async { implicit request =>
    val projectID = Application.getSessionValue(request, "project")
    documentDao.getDocumentByIdAndFilter(id, Json.obj("connection" -> 1)).flatMap {
      res =>
        val connection = (res.getOrElse(Json.obj()) \ "connection").asOpt[String]
        DocumentTypeDao.findByConnectionId(connection.getOrElse(""), projectID, Some(Json.obj("designFilter" -> 1))).flatMap {
          res =>
            val filter = (res.getOrElse(Json.obj()) \ "designFilter").asOpt[List[String]] match {
              case Some(filter) => dbHelper.getJsonObject(for (f <- filter) yield { Json.obj(("fields." + f) -> 1) })
              case None => Json.obj("fields" -> 1)
            }
            documentDao.getDocumentByIdAndFilter(id, filter).map {
              case None => BadRequest("No Document Found")
              case Some(doc) =>
                val fields = (doc \ "fields").asOpt[JsObject].getOrElse(Json.obj())
                Ok(dbHelper.resOK(Json.obj("id" -> id, "fields" -> fields, "connection" -> connection, "project" -> projectID)))
            }
        }
    }
  }

  def getDocumentListView(id: String) = Action.async { implicit request =>
    val projectID = Application.getSessionValue(request, "project")
    documentDao.getDocumentByIdAndFilter(id, Json.obj("connection" -> 1)).flatMap {
      res =>
        val connection = (res.getOrElse(Json.obj()) \ "connection").asOpt[String]
        DocumentTypeDao.findByConnectionId(connection.getOrElse(""), projectID, Some(Json.obj("listDesignFilter" -> 1))).flatMap {
          res =>
            val filter = (res.getOrElse(Json.obj()) \ "listDesignFilter").asOpt[List[String]] match {
              case Some(filter) => dbHelper.getJsonObject(for (f <- filter) yield { Json.obj(("fields." + f) -> 1) })
              case None => Json.obj("fields" -> 1)
            }
            documentDao.getDocumentByIdAndFilter(id, filter).map {
              case None => BadRequest("No Document Found")
              case Some(doc) =>
                val fields = (doc \ "fields").asOpt[JsObject].getOrElse(Json.obj())
                Ok(dbHelper.resOK(Json.obj("id" -> id, "fields" -> fields, "connection" -> connection, "project" -> projectID)))
            }
        }
    }
  }

  def updateDocument(documentID: String) = SecuredAction(ajaxCall = true).async(parse.json) { implicit request =>
    //check if user is creator (&& document is not published yet)

    //get connectionID from DB
    //get FieldElements from Doctype

    //validate document fields against elements from doctype

    //update changedate & author

    //turn to update statement
    val user = request.user.asInstanceOf[GenUser]
    request.session.get("project") match {
      case Some(projectID) => {
        //get connectionID from document (if user is allowed to edit == user is author or user is projectAdmin)

        //check if user is author or admin/projectadmin
        documentDao.getConnIDifUserAuthorized(documentID, user).flatMap { res =>
          res match {
            case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "User is not allowed to edit this document"))))
            case Some(connectionID) => {
              //get FieldElements from Doctype
              val filter = Json.obj("elems" -> 1)
              docTypeDao.findByConnectionId(connectionID, projectID, Some(filter)).flatMap { res =>
                res match {
                  case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Document Type has no elemts - no update possible"))))
                  case Some(docType) => {
                    val elements = (docType \ "elems").as[List[JsObject]]
                    val futureValidationErrors = documentDao.validateDocumentFields(request.body.as[JsObject])

                    (request.body \ "fields") match {
                      case JsUndefined() => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Supplied Document has no elements - no update possible"))))
                      case fields: JsValue => {
                        //iterate over doc type fields
                        //check if field is given in document if so -> check value (type and constraints, yield value)
                        //yielded list of elements -> update statement for document
                        //
                        val checkConstraints = false
                        var location: Option[List[Double]] = None
                        val checkedFields = for (element <- elements) yield {
                          val elementName = (element \ "fname").as[String]
                          val elementType = (element \ "type").as[String]
                          val docVal = fields \ elementName
                          elementType match {
                            case "std" => Json.obj() //omit value - can not be set by user!
                            case "textLine" => {
                              //get value as String
                              docVal.asOpt[String] match {
                                case Some(value) => {
                                  Json.obj("fields." + elementName -> value)
                                }
                                case None => Json.obj("fields." + elementName -> "")
                              }
                            }
                            case "HTML" => {
                              //get value as String
                              Logger.debug("HTML FOUND")
                              docVal.asOpt[String] match {
                                case Some(html) => {
                                  Logger.debug(html)
                                  //sanitize html (against xss,...)
                                  Json.obj("fields." + elementName -> EscapeHelper.sanitizeHTML(html))
                                }
                                case None => Json.obj("fields." + elementName -> "")
                              }
                            }
                            case "img" | "audio" | "video" => {
                              docVal.asOpt[String] match {
                                case Some(media) => {
                                  //... load media from fsgrid... or check constraints at upload
                                  Json.obj("fields." + elementName -> media)
                                }
                                case None => Json.obj("fields." + elementName -> "")
                              }
                            }
                            case "boolean" => {
                              docVal.asOpt[Boolean] match {
                                case Some(bool) => {
                                  //... load media from fsgrid... or check constraints at upload
                                  Json.obj("fields." + elementName -> bool)
                                }
                                case None => Json.obj("fields." + elementName -> false)
                              }
                            }
                            case "number" => {
                              docVal.asOpt[Double] match {
                                case Some(number) => {
                                  Json.obj("fields." + elementName -> number)
                                }
                                case None => Json.obj("fields." + elementName -> 0)
                              }
                            }
                            case "geoLoc" =>
                              val lat = (docVal \ "lat").asOpt[Double] match {
                                case None =>
                                  (docVal \ "lat").asOpt[String] match {
                                    case None => None
                                    case Some(str) => dbHelper.parseDouble(str)
                                  }
                                case Some(lat) => Some(lat)
                              }
                              val lon = (docVal \ "lon").asOpt[Double] match {
                                case None =>
                                  (docVal \ "lon").asOpt[String] match {
                                    case None => None
                                    case Some(str) => dbHelper.parseDouble(str)
                                  }
                                case Some(lon) => Some(lon)
                              }
                              location = Some(List(lon.getOrElse(0), lat.getOrElse(0)))
                              Json.obj("fields." + elementName -> Json.obj(
                                "lat" -> docVal \ "lat",
                                "lon" -> docVal \ "lon",
                                "display_name" -> docVal \ "display_name",
                                "address" -> docVal \ "address"))
                            case _ => Json.obj()
                          }
                        }
                        Logger.debug("CHECKED FIELDS: " + checkedFields)
                        val time = (new java.util.Date).getTime()
                        val titleJson = (request.body \ "title").asOpt[String] match {
                          case Some(title) => Json.obj("title" -> title)
                          case None => Json.obj()
                        }
                        val locationUpdateExtension = location match {
                          case None => Json.obj()
                          case Some(list) => Json.obj("loc" -> list)
                        }
                        val updateQuery = Json.obj("$set" -> (dbHelper.getJsonObject(checkedFields) ++ Json.obj("fields.modifiedAt" -> Json.obj("$date" -> time)) ++ titleJson ++ locationUpdateExtension))
                        val query = Json.obj("_id" -> Json.obj("$oid" -> documentID), "inEdit" -> true, "deleted" -> false)
                        documentDao.updateDocument(query, updateQuery).flatMap {
                          case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                            val result = Json.obj(
                              "result" -> JsString("OK"),
                              "doc" -> BSONFormats.toJSON(doc),
                              "updated" -> updated,
                              "updatedExisting" -> updatedExisting)
                            updated match {
                              case 1 =>
                                futureValidationErrors.map {
                                  case None => Ok(dbHelper.resOK(Json.obj("result" -> result)))
                                  case Some(errors) =>
                                    implicit val validationErrorWriter = new Writes[(String, String)] {
                                      def writes(c: (String, String)): JsValue = {
                                        Json.obj("fieldname" -> c._1, "error" -> c._2)
                                      }
                                    }
                                    Ok(dbHelper.resOK(Json.obj("result" -> result, "validationErrors" -> errors)))
                                }
                              case _ => Future(Ok(dbHelper.resKO(result)))
                            }
                          }
                          case LastError(false, err, code, msg, _, _, _) => {
                            val error = Json.obj(
                              "error" -> JsString(err.getOrElse("unknown")),
                              "code" -> JsNumber(code.getOrElse[Int](0)),
                              "msg" -> JsString(msg.getOrElse("no messsage")))
                            Future(Ok(dbHelper.resKO(error)))
                          }
                        }
                        //Future(Ok(dbHelper.getJsonObject(checkedFields)))
                      }
                      case _ => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Document Document has no elements - no update possible"))))
                    }
                  }
                  //validate document fields against elements from doctype
                }
              }

            }
          }
        }
      }
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
    }
  }

  //request.body.transform(validateBasicSettings) match {
  //case JsSuccess(value, path) => {
  /*val time = (new java.util.Date).getTime()
           request.body.transform(validateBasicSettings andThen
                  dbHelper.generateModified(time) andThen
                  dbHelper.setSearchTitle(title) andThen
                  dbHelper.addChangeUser(user) andThen
                  dbHelper.toMongoUpdate) match {
                  case JsSuccess(query, path) => {
                    Logger.debug("updateQuery: " + query)

                    projectDao.updateProject(projectID, query).map {
                      case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => Ok(dbHelper.resOK(Json.obj(
                        "result" -> JsString("OK"),
                        "doc" -> BSONFormats.toJSON(doc),
                        "updated" -> updated,
                        "updatedExisting" -> updatedExisting)))
                      case LastError(false, err, code, msg, _, _, _) => Ok(dbHelper.resKO(Json.obj(
                        "error" -> JsString(err.getOrElse("unknown")),
                        "code" -> JsNumber(code.getOrElse[Int](0)),
                        "msg" -> JsString(msg.getOrElse("no messsage")))))
                    }

                  }
                  case JsError(errors) => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> errors.toString))))
                }

              }
            }
            //add searchtitle
            //add modifiedDate
            //add modifiedBy
            //}
            // case JsError(errors) => BadRequest(dbHelper.resKO(Json.toJson(errors.toString)))
            //} //TODO!!! */

  /* 	val a = js.transform(jsTransform) match {
case JsSuccess(value,path) => value
case JsError(errors) => errors
}  */

  // save the uploaded file as an attachment of the document with the given id
  def saveImage(id: String) = Action.async(parse.multipartFormData) { request =>
    // here is the future file!
    Logger.debug(request.body.file("image").toString)
    request.body.file("image") match {
      case Some(photo) =>
        //check if photo is type jpeg, jpg, gif or png
        if (checkIfPhoto(photo.contentType.get)) {
          //store original Image
          val metadata = BSONDocument("documentId" -> id)
          // val fileToSave =
          documentDao.gridFSImage.writeFromInputStream(
            DefaultFileToSave(photo.filename, photo.contentType, metadata = metadata),
            new FileInputStream(photo.ref.file)).map {
              res =>
                {
                  Logger.debug("Stored Original: " + toJSON(res.id))
                  ////get photo into BufferedImage
                  Logger.debug(photo.ref.file.getAbsolutePath())
                  val orImg = ImageIO.read(photo.ref.file)
                  Logger.debug("Image: " + orImg.toString())
                  //Resize image
                  val rsImg = Scalr.resize(orImg, 150)
                  Logger.debug("Image: " + rsImg.toString())
                  val os: ByteArrayOutputStream = new ByteArrayOutputStream()
                  //ImageIO.write(orImg, photo.contentType.get, os);
                  //InputStream is = new ByteArrayInputStream(os.toByteArray());
                  //set the resize image to file
                  ImageIO.write(rsImg, "png", os)
                  os.flush()
                  val fis = new ByteArrayInputStream(os.toByteArray())
                  Logger.debug("available: " + fis.available())
                  Logger.debug(res.id.asInstanceOf[BSONObjectID].stringify)
                  documentDao.gridFSImageThumbs.writeFromInputStream(DefaultFileToSave(photo.filename, photo.contentType, metadata = BSONDocument("documentId" -> id, "imageId" -> res.id.asInstanceOf[BSONObjectID].stringify)), fis)
                  Ok(dbHelper.resOK(toJSON(res.id)))
                }
            }.recover {
              case e => Ok(dbHelper.resKO(Json.obj("error" -> "Picture Save Error", "msg" -> ("The uploaded picture could not be saved: " + e.getMessage()))))
            }
        } else {
          Future(Ok(dbHelper.resKO(Json.obj("error" -> "Picture Format Error", "msg" -> "The uploaded File has got a wrong format"))))
        }
      case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "Picture Format Error", "msg" -> "The uploaded File has got a wrong format"))))
    }
  }

  def getImage(id: String) = Action.async { request =>
    // find the matching attachment, if any, and streams it to the client
    val file = documentDao.gridFSImage.find(BSONDocument("_id" -> new BSONObjectID(id)))
    request.getQueryString("inline") match {
      case Some("true") => serve(documentDao.gridFSImage, file, CONTENT_DISPOSITION_INLINE)
      case _ => serve(documentDao.gridFSImage, file)
    }
  }

  def getThumb(id: String) = Action.async { request =>
    // find the matching attachment, if any, and streams it to the client
    val file = documentDao.gridFSImageThumbs.find(BSONDocument("metadata.imageId" -> id))
    Logger.debug(file.toString())
    val x = file.enumerate(1, true)
    Logger.debug(x.toString())
    request.getQueryString("inline") match {
      case Some("true") => serve(documentDao.gridFSImageThumbs, file, CONTENT_DISPOSITION_INLINE)
      case _ => serve(documentDao.gridFSImageThumbs, file)
    }
  }

  def removeImage(id: String) = Action.async {
    //remove image
    documentDao.gridFSImage.remove(new BSONObjectID(id)).map(_ => Ok).recover { case _ => InternalServerError }
  }

  // save the uploaded file as an attachment of the document with the given id
  def saveAudio(id: String) = Action.async(parse.multipartFormData) { request =>
    // here is the future file!
    Logger.debug(request.body.file("audio").toString)
    request.body.file("audio") match {
      case Some(audio) =>
        //check if photo is type jpeg, jpg, gif or png
        if (checkIfAudio(audio.contentType.get)) {
          //store original Image
          val metadata = BSONDocument("documentId" -> id)
          // val fileToSave =
          documentDao.gridFSAudio.writeFromInputStream(
            DefaultFileToSave(audio.filename, audio.contentType, metadata = metadata),
            new FileInputStream(audio.ref.file)).map {
              res =>
                Ok(dbHelper.resOK(toJSON(res.id)))

            }.recover {
              case e => Ok(dbHelper.resKO(Json.obj("error" -> "Audio Save Error", "msg" -> ("The uploaded audio could not be saved: " + e.getMessage()))))
            }
        } else {
          Future(Ok(dbHelper.resKO(Json.obj("error" -> "Audio Format Error", "msg" -> "The uploaded File has got a wrong format"))))
        }
      case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "Audio Format Error", "msg" -> "The uploaded File has got a wrong format"))))
    }
  }

  def getAudio(id: String) = Action.async { request =>
    // find the matching attachment, if any, and streams it to the client
    val file = documentDao.gridFSAudio.find(BSONDocument("_id" -> new BSONObjectID(id)))
    request.getQueryString("inline") match {
      case Some("true") => serve(documentDao.gridFSAudio, file, CONTENT_DISPOSITION_INLINE)
      case _ => serve(documentDao.gridFSAudio, file)
    }
  }

  // save the uploaded file as an attachment of the document with the given id
  def saveVideo(id: String) = Action.async(parse.multipartFormData) { request =>
    // here is the future file!
    Logger.debug(request.body.file("video").toString)
    request.body.file("video") match {
      case Some(video) =>
        //check if photo is type jpeg, jpg, gif or png
        if (checkIfVideo(video.contentType.get)) {
          //store original Image
          val metadata = BSONDocument("documentId" -> id)
          // val fileToSave =
          documentDao.gridFSVideo.writeFromInputStream(
            DefaultFileToSave(video.filename, video.contentType, metadata = metadata),
            new FileInputStream(video.ref.file)).map {
              res =>
                Ok(dbHelper.resOK(toJSON(res.id)))

            }.recover {
              case e => Ok(dbHelper.resKO(Json.obj("error" -> "Audio Save Error", "msg" -> ("The uploaded audio could not be saved: " + e.getMessage()))))
            }
        } else {
          Future(Ok(dbHelper.resKO(Json.obj("error" -> "Audio Format Error", "msg" -> "The uploaded File has got a wrong format"))))
        }
      case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "Audio Format Error", "msg" -> "The uploaded File has got a wrong format"))))
    }
  }

  def getVideo(id: String) = Action.async { request =>
    // find the matching attachment, if any, and streams it to the client
    val file = documentDao.gridFSVideo.find(BSONDocument("_id" -> new BSONObjectID(id)))
    request.getQueryString("inline") match {
      case Some("true") => serve(documentDao.gridFSVideo, file, CONTENT_DISPOSITION_INLINE)
      case _ => serve(documentDao.gridFSVideo, file)
    }
  }
  // check if uploaded photo is type jpeg, jpg, png or gif
  def checkIfPhoto(fileType: String) = {
    fileType match {
      case "image/jpeg" => true
      case "image/jpg" => true
      case "image/png" => true
      case "image/gif" => true
      case _ => false
    }
  }

  // check if uploaded audio is type mp3
  def checkIfAudio(fileType: String) = {
    Logger.debug("AudioFILETYPE: " + fileType)
    fileType match {
      case "audio/mpeg" => true
      case "audio/mp3" => true
      case _ => false
    }
  }

  // check if uploaded video is type mp4
  def checkIfVideo(fileType: String) = {
    Logger.debug("VideoFILETYPE: " + fileType)
    true
    //    fileType match {
    //      case "audio/mpeg" => true
    //      case _ => false
    //    }
  }

  def geocodeAddress(address: String) = Action.async { request =>
    Logger.debug("Encode Address: " + address)
    val addressEncoded = URLEncoder.encode(address, "UTF-8").replaceAllLiterally("+", "%20")
    val geoCodeServiceURL = "http://nominatim.openstreetmap.org/search/" + addressEncoded + "?format=json&addressdetails=1"
    //val geoCodeService = "http://maps.googleapis.com/maps/api/geocode/json?address="+addressEncoded+addressEncoded + "&sensor=true"
    Logger.debug("Call URL: " + geoCodeServiceURL)

    WS.url(geoCodeServiceURL).get().map { response =>
      Logger.debug("response: " + response)
      Ok(dbHelper.resOK(response.json))

    }
  }

  /**
   * Only the creator of a document (and admins & projectadmins) can publish a document
   * returns forbidden if not allowed, ok if published or some Validation errors with res:KO and the errors as JS Array {field:%fieldname%, error:%Message%)
   */
  def publishDocument(id: String) = SecuredAction(ajaxCall = true).async { request =>
    val user = request.user.asInstanceOf[GenUser]
    documentDao.getDocumentById(id).flatMap {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Document does not exist"))))
      case Some(document) =>
        documentDao.userIsAuthorOrAdmin(id, user).flatMap {
          case None => Future(Forbidden(Json.obj("error" -> "Not authorized")))
          case Some(role) =>
            val projectID = (document \ "project").asOpt[String].getOrElse("")
            //TODO Validate Project
            documentDao.validateDocumentFields(document).flatMap {
              case None => //Validation OK - Ready for Publishing
                //check if direct Publishing is enabled in the project
                ProjectDao.queryProject(projectID, Json.obj("directPublish" -> 1)).flatMap {
                  case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Project does not exist"))))
                  case Some(project) =>
                    val query = Json.obj("_id" -> Json.obj("$oid" -> id), "inEdit" -> true, "published" -> false, "deleted" -> false)
                    val updateQuery = (project \ "directPublish").asOpt[Boolean].getOrElse(false) match {
                      case false => Json.obj("$set" -> Json.obj("inEdit" -> false))
                      case true => Json.obj("$set" -> Json.obj("inEdit" -> false, "published" -> true))
                    }
                    documentDao.updateDocument(query, updateQuery).map {
                      case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                        val result = Json.obj(
                          "result" -> JsString("OK"),
                          "doc" -> BSONFormats.toJSON(doc),
                          "updated" -> updated,
                          "updatedExisting" -> updatedExisting)
                        updated match {
                          case 1 => Ok(dbHelper.resOK(Json.obj("result" -> result)))
                          case _ => Ok(dbHelper.resKO(result))
                        }

                      }
                      case LastError(false, err, code, msg, _, _, _) => {
                        val error = Json.obj(
                          "error" -> JsString(err.getOrElse("unknown")),
                          "code" -> JsNumber(code.getOrElse[Int](0)),
                          "msg" -> JsString(msg.getOrElse("no messsage")))
                        Ok(dbHelper.resKO(error))
                      }
                    }

                }
              case Some(errors) =>
                implicit val validationErrorWriter = new Writes[(String, String)] {
                  def writes(c: (String, String)): JsValue = {
                    Json.obj("fieldname" -> c._1, "error" -> c._2)
                  }
                }
                Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "genCMS validationFailed", "validationErrors" -> errors))))
            }
        }
    }
  }

  /**
   * documents to publish can be rejected from being published by projectadmins
   * a message has to be provided for the user (in the form of a posted json with key "msg")
   */
  def rejectPublishDocument(id: String) = SecuredAction(ajaxCall = true).async(parse.json) { request =>
    (request.body \ "msg").asOpt[String] match {
      case None => Future(BadRequest(Json.obj("error" -> "No Message Provided")))
      case Some("") => Future(BadRequest(Json.obj("error" -> "No Message Provided")))
      case Some(msg) =>
        documentDao.getDocumentByIdAndFilter(id, Json.obj("fields.author" -> 1)) flatMap {
          case None => Future(BadRequest(Json.obj("error" -> "Document not found")))
          case Some(storedDoc) =>
            val query = Json.obj("_id" -> Json.obj("$oid" -> id), "inEdit" -> false, "deleted" -> false)
            val updateQuery = Json.obj("$set" -> Json.obj("inEdit" -> true, "published" -> false))
            documentDao.updateDocument(query, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                val result = Json.obj(
                  "result" -> JsString("OK"),
                  "doc" -> BSONFormats.toJSON(doc),
                  "updated" -> updated,
                  "updatedExisting" -> updatedExisting)
                updated match {
                  case 1 =>
                    ((storedDoc \ "fields") \ "author").asOpt[String] match {
                      case Some(author) =>
                      //TODO Send Message to author
                      case None =>
                    }
                    Ok(dbHelper.resOK(Json.obj("result" -> result)))
                  case _ => Ok(dbHelper.resKO(result))
                }
              }
              case LastError(false, err, code, msg, _, _, _) => {
                val error = Json.obj(
                  "error" -> JsString(err.getOrElse("unknown")),
                  "code" -> JsNumber(code.getOrElse[Int](0)),
                  "msg" -> JsString(msg.getOrElse("no messsage")))
                Ok(dbHelper.resKO(error))
              }
            }
        }
    }
  }
  def confirmPublishDocument(id: String) = SecuredAction(ajaxCall = true).async { request =>
    val user = request.user.asInstanceOf[GenUser]
    documentDao.getDocumentById(id).flatMap {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Document does not exist"))))
      case Some(document) =>
        documentDao.userIsAuthorOrAdmin(id, user).flatMap {
          case None => Future(Forbidden(Json.obj("error" -> "Not authorized")))
          case Some("admin") =>
            val query = Json.obj("_id" -> Json.obj("$oid" -> id), "inEdit" -> false, "deleted" -> false)
            val updateQuery = Json.obj("$set" -> Json.obj("inEdit" -> false, "published" -> true))
            documentDao.updateDocument(query, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                val result = Json.obj(
                  "result" -> JsString("OK"),
                  "doc" -> BSONFormats.toJSON(doc),
                  "updated" -> updated,
                  "updatedExisting" -> updatedExisting)
                updated match {
                  case 1 => Ok(dbHelper.resOK(Json.obj("result" -> result)))
                  case _ => Ok(dbHelper.resKO(result))
                }

              }
              case LastError(false, err, code, msg, _, _, _) => {
                val error = Json.obj(
                  "error" -> JsString(err.getOrElse("unknown")),
                  "code" -> JsNumber(code.getOrElse[Int](0)),
                  "msg" -> JsString(msg.getOrElse("no messsage")))
                Ok(dbHelper.resKO(error))
              }
            }
          case Some(otherRole) => Future(Forbidden(Json.obj("error" -> "Not authorized")))
        }
    }
  }
  def unpublishDocument(id: String) = SecuredAction(ajaxCall = true).async { request =>
    //TODO check if document is used as job description for an author role - if so unrelease is not possible!
    val user = request.user.asInstanceOf[GenUser]
    documentDao.getDocumentById(id).flatMap {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Document does not exist"))))
      case Some(document) =>
        documentDao.userIsAuthorOrAdmin(id, user).flatMap {
          case None => Future(Forbidden(Json.obj("error" -> "Not authorized")))
          case Some(role) =>
            val query = Json.obj("_id" -> Json.obj("$oid" -> id), "inEdit" -> false, "deleted" -> false)
            val updateQuery = Json.obj("$set" -> Json.obj("inEdit" -> true, "published" -> false))
            documentDao.updateDocument(query, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                val result = Json.obj(
                  "result" -> JsString("OK"),
                  "doc" -> BSONFormats.toJSON(doc),
                  "updated" -> updated,
                  "updatedExisting" -> updatedExisting)
                updated match {
                  case 1 => Ok(dbHelper.resOK(Json.obj("result" -> result)))
                  case _ => Ok(dbHelper.resKO(result))
                }

              }
              case LastError(false, err, code, msg, _, _, _) => {
                val error = Json.obj(
                  "error" -> JsString(err.getOrElse("unknown")),
                  "code" -> JsNumber(code.getOrElse[Int](0)),
                  "msg" -> JsString(msg.getOrElse("no messsage")))
                Ok(dbHelper.resKO(error))
              }
            }
        }
    }
  }

  def deleteDocument(id: String) = SecuredAction(ajaxCall = true).async { request =>
    //TODO check if document is used as job description for an author role - if so deleted is not possible!
    val user = request.user.asInstanceOf[GenUser]
    documentDao.getDocumentById(id).flatMap {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "Document does not exist"))))
      case Some(document) =>
        documentDao.userIsAuthorOrAdmin(id, user).flatMap {
          case None => Future(Forbidden(Json.obj("error" -> "Not authorized")))
          case Some(role) =>
            val query = Json.obj("_id" -> Json.obj("$oid" -> id), "deleted" -> false)
            val updateQuery = Json.obj("$set" -> Json.obj("inEdit" -> false, "published" -> false, "deleted" -> true))
            documentDao.updateDocument(query, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                val result = Json.obj(
                  "result" -> JsString("OK"),
                  "doc" -> BSONFormats.toJSON(doc),
                  "updated" -> updated,
                  "updatedExisting" -> updatedExisting)
                updated match {
                  case 1 => Ok(dbHelper.resOK(Json.obj("result" -> result)))
                  case _ => Ok(dbHelper.resKO(result))
                }

              }
              case LastError(false, err, code, msg, _, _, _) => {
                val error = Json.obj(
                  "error" -> JsString(err.getOrElse("unknown")),
                  "code" -> JsNumber(code.getOrElse[Int](0)),
                  "msg" -> JsString(msg.getOrElse("no messsage")))
                Ok(dbHelper.resKO(error))
              }
            }
        }
    }
  }

  def addTag(documentID: String, tag: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    val user = request.user.asInstanceOf[GenUser]
    documentDao.userIsAuthorOrAdmin(documentID, user).flatMap {
      case None => Future(Forbidden(Json.obj("error" -> "Not authorized")))
      case Some(role) =>
        documentDao.addTag(documentID, tag).map {
          case (true, msg) => Ok(dbHelper.resOK(Json.obj("msg" -> msg)))
          case (false, msg) => Ok(dbHelper.resKO(Json.obj("error" -> msg)))
        }
    }
  }

  def removeTag(documentID: String, tag: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    val user = request.user.asInstanceOf[GenUser]
    documentDao.userIsAuthorOrAdmin(documentID, user).flatMap {
      case None => Future(Forbidden(Json.obj("error" -> "Not authorized")))
      case Some(role) =>
        documentDao.removeTag(documentID, tag).map {
          case (true, msg) => Ok(dbHelper.resOK(Json.obj("msg" -> msg)))
          case (false, msg) => Ok(dbHelper.resKO(Json.obj("error" -> msg)))
        }
    }
  }

  def findDocsByMapBox(minLng: Double, minLat: Double, maxLng: Double, maxLat: Double) = Action.async { implicit request =>

    //documentDao.getNearLocationsWithDistance(long, lat, projectID).map { result =>
    documentDao.getLocationsInBox(minLng, minLat, maxLng, maxLat).map { result =>
      {
        Ok(dbHelper.resOK((Json.toJson(result))))
      }
    }
  }

  /**
   * Create a new Document according to supplied DocType and connectionID
   */
  def createDocTypeStyle(name: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    val user = request.user.asInstanceOf[GenUser]
    val allowed = user.isAdmin

    allowed match {
      case false => Future(Forbidden(Json.obj("error" -> "Not authorized")))
      case true =>
        //insert new style
        //return with id
        val style = Json.obj("deleted" -> false, "name" -> name, "style" -> "", "_id" -> Json.obj("$oid" -> JsString(BSONObjectID.generate.stringify)))
        docTypeDao.createDocTypeStyle(style).map {
          case true =>
            Ok(dbHelper.resOK(Json.obj("style" -> style)))
          case false =>
            Ok(dbHelper.resKO(Json.obj("error" -> "Style could not be created")))
        }
    }
  }

  /**
   * udate doctype style
   */
  def updateDocTypeStyle() = SecuredAction(ajaxCall = true).async(parse.json) { implicit request =>
    val user = request.user.asInstanceOf[GenUser]
    val allowed = user.isAdmin
    Future(Ok(dbHelper.resOK(Json.obj())))
    allowed match {
      case false => Future(Forbidden(Json.obj("error" -> "Not authorized")))
      case true =>
        val id = ((request.body \ "_id") \ "$oid").asOpt[String].getOrElse("")
        val name = (request.body \ "name").asOpt[String].getOrElse("")
        val style = (request.body \ "style").asOpt[String].getOrElse("")

        id match {
          case "" => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "id missing"))))
          case styleID =>
            val updateQuery = Json.obj("$set" -> Json.obj("name" -> name, "style" -> style))
            val query = Json.obj("_id" -> Json.obj("$oid" -> styleID))
            docTypeDao.updateDocTypeStyle(query, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                val result = Json.obj(
                  "result" -> JsString("OK"),
                  "doc" -> BSONFormats.toJSON(doc),
                  "updated" -> updated,
                  "updatedExisting" -> updatedExisting)
                updated match {
                  case 1 =>
                    Ok(dbHelper.resOK(Json.obj("result" -> result)))
                  case _ =>
                    Ok(dbHelper.resKO(result))
                }
              }
              case LastError(false, err, code, msg, _, _, _) => {
                val error = Json.obj(
                  "error" -> JsString(err.getOrElse("unknown")),
                  "code" -> JsNumber(code.getOrElse[Int](0)),
                  "msg" -> JsString(msg.getOrElse("no messsage")))
                Ok(dbHelper.resKO(error))
              }
            }
        }
    }
  }

  /**
   * delete doctype style
   */
  def deleteDocTypeStyle(id: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    val user = request.user.asInstanceOf[GenUser]
    val allowed = user.isAdmin
    Future(Ok(dbHelper.resOK(Json.obj())))
    allowed match {
      case false => Future(Forbidden(Json.obj("error" -> "Not authorized")))
      case true =>
        id match {
          case "" => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "id missing"))))
          case styleID =>
            val updateQuery = Json.obj("$set" -> Json.obj("deleted" -> true))
            val query = Json.obj("_id" -> Json.obj("$oid" -> styleID))
            docTypeDao.updateDocTypeStyle(query, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
                val result = Json.obj(
                  "result" -> JsString("OK"),
                  "doc" -> BSONFormats.toJSON(doc),
                  "updated" -> updated,
                  "updatedExisting" -> updatedExisting)
                updated match {
                  case 1 =>
                    Ok(dbHelper.resOK(Json.obj("result" -> result)))
                  case _ =>
                    Ok(dbHelper.resKO(result))
                }
              }
              case LastError(false, err, code, msg, _, _, _) => {
                val error = Json.obj(
                  "error" -> JsString(err.getOrElse("unknown")),
                  "code" -> JsNumber(code.getOrElse[Int](0)),
                  "msg" -> JsString(msg.getOrElse("no messsage")))
                Ok(dbHelper.resKO(error))
              }
            }
        }
    }
  }

  def getDocTypeStylesFull() = Action.async { implicit request =>
    val query = Json.obj("deleted" -> false)
    docTypeDao.queryDocTypeStyles(query, Some(Json.obj())).map { res =>
      Ok(dbHelper.resOK(Json.obj("styles" -> res)))
    }
  }

  def getDocTypeStylesKeyValue() = Action.async { implicit request =>
    val query = Json.obj("deleted" -> false)
    val filter = Json.obj("_id" -> 1, "name" -> 1)
    docTypeDao.queryDocTypeStyles(query, Some(filter)).map { res =>
      Ok(dbHelper.resOK(Json.obj("styles" -> res)))
    }
  }

}