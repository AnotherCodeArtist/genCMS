package controllers

import scala.concurrent.Future
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.libs.json.Reads._
import play.api.mvc._
import play.api.mvc.Controller
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.BSONFormats._
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.core.commands.LastError
import reactivemongo.core.errors.DatabaseException
import securesocial.core.Authorization
import securesocial.core.Identity
import securesocial.core.Identity
import securesocial.core.SecureSocial
import securesocial.core.SecuredRequest
import service.DBHelper
import service.DocumentTypeDao
import service.GenUser
import service.ProjectDao
import service.UserDao
import service.DocumentDao
import play.api.i18n.Messages
import reactivemongo.bson.BSONObjectID

object ProjectController extends Controller with SecureSocial {

  //Projects collection
  //def projects = db.collection[JSONCollection]("projects")
  def projectDao = ProjectDao

  val dbHelper = DBHelper

  val validateBasicSettings = (
    (__ \ 'title).json.copyFrom((__ \ 'title).json.pick[JsString]) and
    (__ \ 'description).json.copyFrom((__ \ 'description).json.pick[JsString]) and
    (__ \ 'public).json.copyFrom((__ \ 'public).json.pick[JsBoolean]) and
    (__ \ 'editable).json.copyFrom((__ \ 'editable).json.pick[JsBoolean]) and
    (__ \ 'directPublish).json.copyFrom((__ \ 'directPublish).json.pick[JsBoolean]) and
    (__ \ 'language).json.copyFrom((__ \ 'language).json.pick[JsString])) reduce
  //def

  def createProject() = SecuredAction(ajaxCall = true).async(parse.text) { implicit request =>
    //TODO
    /*
     * check if name given by user is already used
     * load default project from DB
     * generate new id DONE
     * add name from user input DONE
     * store new project to db DONE
     * reply user the generated project
     * 
     */
    Logger.debug("project: " + request.body)

    val defaultProject = Json.obj(
      "description" -> "",
      "public" -> false,
      "editable" -> false,
      "language" -> "en",
      "editorial" -> "",
      "directPublish" -> false,
      "tags" -> JsArray(),
      "authorroles" -> JsArray() //TODO Add rest of required options
      //"authorroles" -> JsArray(Seq(Json.obj("name" -> "default", "document" -> ""))) //TODO Add rest of required options
      )

    val language = request.cookies.get("locale").map(cookie => cookie.value).getOrElse("en")
    val project = defaultProject deepMerge (Json.obj("titleSearch" -> request.body.toLowerCase(), "title" -> request.body, "language" -> language))
    val currentUser = request.user.identityId.userId
    //val currentUser = Application.getSessionValue(request, "email")

    project.transform((dbHelper.addMongoId and dbHelper.addCreationAndModificationDate and dbHelper.addCreateUser(currentUser) and dbHelper.addChangeUser(currentUser)) reduce).map {
      project =>
        val saved = ProjectDao.save(project)
        saved.map { Ok(_) }
    }.recoverTotal { err =>
      Future.successful(BadRequest(dbHelper.resKO(JsError.toFlatJson(err))))
    }
  }

  def checkTitle(title: String) = Action.async { implicit request =>
    projectDao.isTitleInUse(title).map {
      case true => Ok(Json.obj("used" -> true, "unique" -> false))
      case false => Ok(Json.obj("used" -> false, "unique" -> true))
    }
  }

  /**
   * Returns the projecttitles as a JSON Object with key value pairs (projectID:Title)
   */
  def getHeaderInfos() = UserAwareAction.async { implicit request =>
    projectDao.getProjectTitles().flatMap { projectTitles =>
      projectDao.getProjectAuthorRoles().flatMap { projectRoles =>
        //TODO
        projectDao.getProjectTags().flatMap { projectTags =>
          DocumentTypeDao.getAvailableTemplatesByProjectID(request.session.get("project")).flatMap { templates =>
            val projectID = request.session.get("project").getOrElse("")

            val localeFilter = Json.obj("connectedProjects." + projectID -> Json.obj("$exists" -> true, "$not" -> Json.obj("$size" -> 0)));
            DocumentTypeDao.getDocumentTypeLocales(localeFilter).flatMap { locales =>
              {

                //Ok(Json.prettyPrint(Json.toJson(result))) //c.mkString(" | "))
                projectDao.queryProject(projectID, Json.obj("editorial" -> 1)).flatMap { res =>
                  val editorial = res match {
                    case None => ""
                    case Some(project) => (project \ "editorial").asOpt[String].getOrElse("")
                  }
                  //get tags structure (navigation) for current project
                  projectDao.getProjectTagsStruktur(projectID).flatMap { navigationStructure =>
                    request.user match {
                      case None => Future(Ok(dbHelper.resOK(Json.obj(
                        "titles" -> projectTitles,
                        "templates" -> templates,
                        "roles" -> projectRoles,
                        "tags" -> projectTags,
                        "navigationStructure" -> navigationStructure,
                        "user" -> "",
                        "projectID" -> projectID,
                        "locale" -> Messages("genCMS.locale"),
                        "localeLabels" -> locales))))
                      case Some(user) =>
                        UserDao.getUser(user.identityId.userId).map { res =>
                          res match {
                            case None =>
                              Ok(dbHelper.resOK(Json.obj(
                                "titles" -> projectTitles,
                                "templates" -> templates,
                                "roles" -> projectRoles,
                                "tags" -> projectTags,
                                "navigationStructure" -> navigationStructure,
                                "user" -> "",
                                "projectID" -> projectID,
                                "locale" -> Messages("genCMS.locale"),
                                "localeLabels" -> locales)))
                            case Some(user) =>
                              Ok(dbHelper.resOK(Json.obj(
                                "titles" -> projectTitles,
                                "templates" -> templates,
                                "roles" -> projectRoles,
                                "tags" -> projectTags,
                                "navigationStructure" -> navigationStructure,
                                "user" -> user,
                                "projectID" -> projectID,
                                "editorial" -> editorial,
                                "locale" -> Messages("genCMS.locale"),
                                "localeLabels" -> locales)))
                          }
                        }
                    }
                  }
                }
              }
              //return logged in user
            }
          }
        }
      }
    }
  }

  /**
   * Returns the projecttitles as a JSON Object with key value pairs (projectID:Title)
   */
  def getProjectTitles() = Action.async { implicit request =>
    projectDao.getProjectTitles().map { res =>
      Ok(dbHelper.resOK(res))
    }
  }

  /**
   * Returns the project authorroles as a JSON Object with key value pairs (projectID:[authorroles])
   */
  def getProjectAuthorRoles() = Action.async { implicit request =>
    projectDao.getProjectAuthorRoles().map { res =>
      Ok(dbHelper.resOK(res))
    }
  }

  /**
   * Returns the project tags as a JSON Object with key value pairs (projectID:[tags])
   */
  def getProjectTags() = Action.async { implicit request =>
    projectDao.getProjectTags().map { res =>
      Ok(dbHelper.resOK(res))
    }
  }

  /**
   * Returns the used tags (published DOCS) of the selected Project with count as JSON Object like:
   * {"res":"OK","data":{"tags":[{"_id":"SOMETAG","count":5},{"_id":"ANOTHERTAG","count":1}]}}
   */
  def getSelectedProjectTags() = Action.async(parse.json) { implicit request =>
    request.session.get("project") match {
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
        Logger.debug("DOC QUERY: " + documentQuery);
        projectDao.getSelectedProjectTagsWithCount(documentQuery).map { res =>
          Ok(dbHelper.resOK(Json.obj("tags" -> res)))
        }
      case None => Future(Ok(dbHelper.resKO(Json.toJson("no project selected"))))
    }
  }

  /**
   * Returns the used tags (published DOCS) of the selected Project with count as JSON Object like:
   * {"res":"OK","data":{"tags":[{"_id":"SOMETAG","count":5},{"_id":"ANOTHERTAG","count":1}]}}
   */
  def saveSelectedProjectTagsStructure() = Action.async(parse.json) { implicit request =>
    request.session.get("project") match {
      case Some(projectID) =>
        (request.body \ "tags").asOpt[List[JsObject]] match {
          case Some(tagList) =>
            Logger.debug("TAGS: " + tagList);
            val updateQuery = Json.obj("$set" -> Json.obj("tagsStructure" -> (tagList)))
            projectDao.updateProject(projectID, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                Ok(dbHelper.resOK(Json.obj("msg" -> "Tags Saved")))
              case LastError(false, err, code, msg, _, _, _) =>
                BadRequest(dbHelper.resKO(Json.obj("error" -> "Tags Could not be saved")))
            }
            Future(Ok(""))
          case None =>
            Future(BadRequest(""))
        }
      //        val documentQuery = Json.obj("project" -> projectID, "deleted" -> false, "published" -> true) ++ connectionQueryExt ++ tagsQueryExt
      //        Logger.debug("DOC QUERY: " + documentQuery);
      //        projectDao.getSelectedProjectTagsWithCount(documentQuery).map { res =>
      //          Ok(dbHelper.resOK(Json.obj("tags" -> res)))
      //        }
      case None => Future(Ok(dbHelper.resKO(Json.toJson("no project selected"))))
    }
  }

  /**
   * Returns the used docType - Connections (published DOCS) of the selected Project with count as JSON Object like:
   * {
   * "res":"OK",
   * "data":{
   * "connections":[
   * {
   * "_id":"5305ba905f000046025c9101",
   * "count":1,
   * "name":"Point of Interest"
   * },
   * .....
   * ]}}
   */
  def getSelectedProjectConnections() = Action.async(parse.json) { implicit request =>
    //val projectID = "52d13eb49d0000d9016df9bf"
    request.session.get("project") match {
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
          DocumentTypeDao.getAllTypesNames(projectID).map { docTypeResMap =>
            val result = for (conn <- connRes) yield {
              val id = (conn \ "_id").asOpt[String].getOrElse("")
              conn ++ Json.obj("name" -> (docTypeResMap.getOrElse(id, "").asInstanceOf[String]))
            }
            Ok(dbHelper.resOK(Json.obj("connections" -> result)))
          }

        }
      case None => Future(Ok(dbHelper.resKO(Json.toJson("no project selected"))))
    }
  }

  def getProjects(page: Int, perPage: Int) = SecuredAction(ajaxCall = true).async { implicit request =>
    val user = request.user.asInstanceOf[GenUser]
    val projects = projectDao.getProjects(page, perPage, user)
    projects.map {
      //projects => Ok(Json.obj("projects" -> projects))
      projects => Ok(Json.toJson(projects))
    }
  }

  def selectProject(projectID: String) = Action.async { implicit request =>
    val project = projectDao.getProjectById(projectID)
    project.map {
      project =>
        if (project == null) {
          Ok(dbHelper.resKO(Json.toJson("no such project")))
        } else {
          //          val jsresult: Option[String] = (project \ "title").asOpt[String]
          //          val title = jsresult.getOrElse("")
          Ok(dbHelper.resOK(Json.toJson("Project Selected"))).withSession(session + ("project" -> projectID))
        }
    }
  }

  def getSelectedProject() = Action.async { implicit request =>
    request.session.get("project") match {
      case Some(projectID) => {
        projectDao.getProjectById(projectID).flatMap {
          project =>
            projectDao.getDistinctProjectTags(projectID).flatMap {
              distinctTags =>
                projectDao.getProjectTagsStruktur(projectID).flatMap {
                  tagStruktur =>
                    DocumentTypeDao.queryDocTypeStyles(Json.obj("deleted"->false), Some(Json.obj())).map {
                      styles =>
                        Ok(dbHelper.resOK(Json.obj("project" -> project, "distinctTags" -> distinctTags, "ordererdTags" -> tagStruktur, "styles" -> styles)))
                    }
                }
            }
        }
      }
      case None => Future(Ok(dbHelper.resKO(Json.toJson("no project selected"))))
    }

  }

  def getTemplatesSelectedProject() = Action.async { implicit request =>
    request.session.get("project") match {
      case Some(id) => {
        DocumentTypeDao.getAvailableTemplatesByProjectID(Some(id)).map {
          project =>
            Ok(dbHelper.resOK(Json.obj("templates" -> project)))
        }
      }
      case None => Future(Ok(dbHelper.resKO(Json.toJson("no project selected"))))
    }

  }

  def updateBasicSettings() = SecuredAction(ajaxCall = true).async(parse.json) { implicit request =>
    request.session.get("project") match {
      case Some(projectID) => {
        val user = request.user.identityId.userId
        //request.body.transform(validateBasicSettings) match {
        //case JsSuccess(value, path) => {
        val time = (new java.util.Date).getTime()
        val title = ((request.body \ "title").asOpt[String]).getOrElse("")
        projectDao.isTitleInUse(title, projectID).flatMap {
          case true => Future(BadRequest(
            dbHelper.resKO(Json.obj("error" -> "Title is already in use"))))
          case false => {
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
      }
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
    }
  }

  def setEditorial(docID: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    request.session.get("project") match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(projectID) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(projectID)) {
          //check if document is available and published!
          val docQuery = Json.obj("_id" -> Json.obj("$oid" -> docID), "deleted" -> false, "inEdit" -> false, "published" -> true)
          val filter = Json.obj("_id" -> 1)
          DocumentDao.queryDocument(docQuery, Some(filter)).flatMap {
            case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "Document not found or not published"))))
            case Some(doc) =>
              val updateQuery = Json.obj("$set" -> Json.obj("editorial" -> docID))
              projectDao.updateProject(projectID, updateQuery).map {
                case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                  Ok(dbHelper.resOK(Json.obj()))
                case LastError(false, err, code, msg, _, _, _) =>
                  Ok(dbHelper.resKO(Json.obj("error" -> (err + " " + msg))))
              }
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / projectadmin user can not add tags to project
        }
    }
  }

  def setAuthorRoleJobDescription(role: String, docID: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    request.session.get("project") match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(projectID) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(projectID)) {
          //check if document is available and published!
          val docQuery = Json.obj("_id" -> Json.obj("$oid" -> docID), "deleted" -> false, "inEdit" -> false, "published" -> true)
          val filter = Json.obj("_id" -> 1)
          DocumentDao.queryDocument(docQuery, Some(filter)).flatMap {
            case None => Future(Ok(dbHelper.resKO(Json.obj("error" -> "Document not found or not published"))))
            case Some(doc) =>
              val updateQuery = Json.obj("$set" -> Json.obj("authorroles.$.jobdoc" -> docID))
              projectDao.updateQueriedProject(dbHelper.toObjectId(projectID) ++ Json.obj("authorroles.name" -> role), updateQuery).map {
                case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                  Ok(dbHelper.resOK(Json.obj()))
                case LastError(false, err, code, msg, _, _, _) =>
                  Ok(dbHelper.resKO(Json.obj("error" -> (err + " " + msg))))
              }
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / projectadmin user can not add tags to project
        }
    }

  }

  def addTag(tag: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    request.session.get("project") match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(projectID) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(projectID)) {
          projectDao.addTag(projectID, tag).map {
            case (true, msg) => Ok(dbHelper.resOK(Json.obj("msg" -> msg)))
            case (false, msg) => Ok(dbHelper.resKO(Json.obj("error" -> msg)))
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / projectadmin user can not add tags to project
        }
    }
  }

  def removeTag(tag: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    request.session.get("project") match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(projectID) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(projectID)) {
          projectDao.removeTag(projectID, tag).map {
            case (true, msg) => Ok(dbHelper.resOK(Json.obj("msg" -> msg)))
            case (false, msg) => Ok(dbHelper.resKO(Json.obj("error" -> msg)))
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / projectadmin user can not remove tags from project
        }
    }
  }

  def addAuthorRole(authorRole: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    request.session.get("project") match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(projectID) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(projectID)) {
          projectDao.addAuthorRole(projectID, authorRole).map {
            case (true, msg) => Ok(dbHelper.resOK(Json.obj("msg" -> msg)))
            case (false, msg) => Ok(dbHelper.resKO(Json.obj("error" -> msg)))
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / projectadmin user can not add tags to project
        }
    }
  }

  def removeAuthorRole(roleName: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    //TODO check if there are users with this role!
    request.session.get("project") match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(projectID) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(projectID)) {
          projectDao.removeAuthorRole(projectID, roleName).map {
            case (true, msg) => Ok(dbHelper.resOK(Json.obj("msg" -> msg)))
            case (false, msg) => Ok(dbHelper.resKO(Json.obj("error" -> msg)))
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / projectadmin user can not remove tags from project
        }
    }
  }

  def getProjectStyle() = Action.async { implicit request =>
    DocumentTypeDao.queryDocTypeStyles(Json.obj("deleted"->false), Some(Json.obj("_id" -> 1, "style" -> 1))).map { res =>
      val stylesheet = res.foldLeft("")((acc, x) => {
        val id = "genCMS" + ((x \ "_id") \ "$oid").asOpt[String].getOrElse("")
        val style = ((x \ "style")).asOpt[String].getOrElse("")
        acc ++ " ." + id + "{ " + style + " }"
      })
      Ok(stylesheet)
    }

  }

  def getProjectSettings(id: String) = TODO
  def getProject(id: String) = TODO
  def getNewDocument() = TODO
  def releaseDocuments() = TODO

}