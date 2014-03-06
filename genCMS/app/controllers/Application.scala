package controllers

import scala.concurrent.Future

import play.api.Logger
import play.api.Play.current
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import play.modules.reactivemongo.MongoController
import securesocial.core.Authorization
import securesocial.core.Identity
import securesocial.core.SecureSocial
import service.DBHelper
import service.GenUser
import service.ProjectDao
import service.UserDao

case class IsAdmin() extends Authorization {
  def isAuthorized(identity: Identity): Boolean = {
    identity match {
      case user: GenUser => user.isAdmin
      case _ => false //no user logged in - not authenticated and not authorized
    }
  }
}

object Application extends Controller with MongoController with SecureSocial {

  implicit def user(implicit request: RequestHeader): Option[Identity] = {
    SecureSocial.currentUser
  }

  val userDao = UserDao
  val dbHelper = DBHelper

  def changeLanguage(langCode: String) = Action { implicit request =>
    implicit val lang = langCode match {
      case "de" => Lang("de")
      case "en" => Lang("en")
      case "fr" => Lang("fr")
      case "es" => Lang("es")
      case _ => Lang("en")
    }
    Ok(Messages("hello.world")).withLang(lang)
  }

  def index = UserAwareAction.async { implicit request =>
    request.user match {
      case None => Future(Redirect(securesocial.controllers.routes.LoginPage.login()).withNewSession)
      case Some(user) =>
        val projectId = getSessionValue(request, "project")
        if (projectId != "") {
          ProjectDao.getProjectTitleById(projectId).map {
            title =>
              title match {
                case Some(title) => Ok(views.html.index(title, true))
                case None => Ok(views.html.index("", true))
              }
          }
        } else {
          Future(Ok(views.html.index("", false)))
        }
    }
  }

  def indexplain = UserAwareAction { implicit request =>
    Ok(views.html.indexPlain())
  }

  def getSessionValue(request: RequestHeader, field: String): String = {
    request.session.get(field).map {
      value => value
    }.getOrElse("")
  }

  /**
   * A function to make users to admins = full rights
   * can only be executed by admins
   */
  def setAdmin(email: String) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    userDao.makeAdmin(email).map {
      res =>
        res match {
          case (false, result) => Ok(dbHelper.resKO(Json.obj("error" -> result)))
          case (true, result) => Ok(dbHelper.resOK(Json.obj("msg" -> result)))
        }
    }
  }

  def removeAdmin(email: String) = SecuredAction(ajaxCall = true, IsAdmin()).async { implicit request =>
    userDao.removeAdmin(email).map {
      res =>
        res match {
          case (false, result) => Ok(dbHelper.resKO(Json.obj("error" -> result)))
          case (true, result) => Ok(dbHelper.resOK(Json.obj("msg" -> result)))
        }
    }
  }

  def setProjectadmin(email: String, projectID: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    (projectID match {
      case "" => request.session.get("project")
      case id => Some(id)
    }) match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(project) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(project)) {
          userDao.makeProjectAdmin(email, project).map {
            res =>
              res match {
                case (false, result) => Ok(dbHelper.resKO(Json.obj("error" -> result)))
                case (true, result) => Ok(dbHelper.resOK(Json.obj("msg" -> result)))
              }
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / project admin user can not update other users
        }
    }
  }
  def removeProjectadmin(email: String, projectID: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    (projectID match {
      case "" => request.session.get("project")
      case id => Some(id)
    }) match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(project) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(project)) {
          userDao.removeProjectAdmin(email, project).map {
            res =>
              res match {
                case (false, result) => Ok(dbHelper.resKO(Json.obj("error" -> result)))
                case (true, result) => Ok(dbHelper.resOK(Json.obj("msg" -> result)))
              }
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / project admin user can not update other users
        }
    }
  }
  def setAuthor(email: String, role: String, projectID: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    (projectID match {
      case "" => request.session.get("project")
      case id => Some(id)
    }) match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(project) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(project)) {
          userDao.makeProjectAuthor(email, project, role).map {
            res =>
              res match {
                case (false, result) => Ok(dbHelper.resKO(Json.obj("error" -> result)))
                case (true, result) => Ok(dbHelper.resOK(Json.obj("msg" -> result)))
              }
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / project admin user can not update other users
        }
    }
  }
  def removeAuthor(email: String, projectID: String) = SecuredAction(ajaxCall = true).async { implicit request =>
    (projectID match {
      case "" => request.session.get("project")
      case id => Some(id)
    }) match {
      case None => Future(BadRequest(dbHelper.resKO(Json.obj("error" -> "No Project Selected"))))
      case Some(project) =>
        val user = request.user.asInstanceOf[GenUser]
        if (user.isProjectAdmin(project)) {
          userDao.removeProjectAuthor(email, project).map {
            res =>
              res match {
                case (false, result) => Ok(dbHelper.resKO(Json.obj("error" -> result)))
                case (true, result) => Ok(dbHelper.resOK(Json.obj("msg" -> result)))
              }
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin / project admin user can not update other users
        }
    }
  }

  //Actions to deliver html parts

  def getForbiddenPage() = UserAwareAction { implicit request =>
    Ok(views.html.forbidden())
  }

  def getDocTypeEditorPage() = SecuredAction(ajaxCall = true, IsAdmin()) { implicit request =>
    Ok(views.html.documentType.docTypeEdit())
  }

  def getDocTypeDesignEditor(listDesign: Boolean) = SecuredAction(ajaxCall = true, IsAdmin()) { implicit request =>
    Ok(views.html.documentType.docTypeDesignEdit(listDesign))
  }

  def getUserAccountPage() = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(views.html.userAccountSettings())
  }

  //def getUserManagementPage = SecuredAction(ajaxCall = true) { implicit request =>
  def getUserManagementPage = Action { implicit request =>
    Ok(views.html.manageUsers())
  }

  def getProjectNewPage = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(views.html.project.projectNew())
  }

  def getProjectEditPage = SecuredAction(ajaxCall = true) { implicit request =>
    Ok(views.html.project.projectEdit())
  }

  def getProjectsList = UserAwareAction { implicit request =>
    Ok(views.html.project.projectsList())
  }

  def getHeader = UserAwareAction.async { implicit request =>
    val user = request.user.getOrElse(null)
    val projectId = request.session.get("project")
    projectId match {
      case None => {
        Future(Ok(views.html.header("", false)))
      }
      case Some(id) => {
        val projectId = getSessionValue(request, "project")
        ProjectDao.getProjectTitleById(projectId).map {
          title =>
            val projectTitle = title.getOrElse("")
            Ok(views.html.header(projectTitle, true))
        }
      }
    }
  }

  def getDocTypeConnector = SecuredAction(ajaxCall = true, IsAdmin()) { implicit request =>
    request.session.get("project") match {
      case Some(projectID) => {
        Ok(views.html.documentType.docTypeConnectToProject(projectID))
      }
      case None => {
        BadRequest(dbHelper.resKO(Json.obj("error" -> "no project selected")))
      }
    }
  }

  def getSelectNewDocument = SecuredAction(ajaxCall = true) { implicit request =>
    request.session.get("project") match {
      case Some(projectID) => {
        request.user.asInstanceOf[GenUser].isProjectAuthor(projectID) match {
          case true => //allowed to create documents
            Ok(views.html.document.documentNew())
          case false => //not allowed to create documents
            //{"error":"Not authorized"}
            Forbidden(Json.obj("error" -> "Not authorized"))
        }
      }
      case None => {
        BadRequest(dbHelper.resKO(Json.obj("error" -> "no project selected")))
      }
    }
  }

  def getDocumentEditor = SecuredAction(ajaxCall = true) { implicit request =>
    request.session.get("project") match {
      case Some(projectID) => {
        Ok(views.html.document.documentEdit())
      }
      case None => {
        BadRequest(dbHelper.resKO(Json.obj("error" -> "no project selected")))
      }
    }
  }

  def getDocumentList = UserAwareAction { implicit request =>
    Ok(views.html.document.documentList())
  }

  def getDocumentListUnreleased = UserAwareAction { implicit request =>
    Ok(views.html.document.documentListToPublish())
  }

  def getUsers(page: Int, perPage: Int, projectOnly: Boolean = true, filteredOnly: Boolean = false, admin: Boolean = false, projectAdmin: Boolean = false, author: Boolean = false, orderBy: String = "userid", asc: Boolean = true) = Action.async { implicit request =>
    Logger.debug("getUsers!!!!")
    Logger.debug(request.getQueryString("firstName").getOrElse(""))
    Logger.debug(request.getQueryString("lastName").getOrElse(""))
    Logger.debug(request.getQueryString("userID").getOrElse(""))
    val projectId = request.session.get("project").getOrElse("")
    userDao.getUsers(page, perPage, projectId, projectOnly, filteredOnly, admin, projectAdmin, author, orderBy, asc, request.getQueryString("firstName"), request.getQueryString("lastName"), request.getQueryString("userID")).map {
      res => Ok(dbHelper.resOK(res))
    }
  }

  def getUser(email: String) = UserAwareAction.async { implicit request =>
    val projectID = request.session.get("project").getOrElse("")
    email match {
      case "" => //return logged in user
        request.user match {
          case None => Future(Ok(dbHelper.resOK(Json.obj("user" -> ""))))
          case Some(user) =>
            userDao.getUser(user.identityId.userId).map { res =>
              res match {
                case None => Ok(dbHelper.resKO(Json.obj("error" -> "No User found")))
                case Some(user) => Ok(dbHelper.resOK(Json.obj("user" -> user, "projectID" -> projectID)))
              }
            }
        }
      case userid =>
        userDao.getUser(email).map { res =>
          res match {
            case None => Ok(dbHelper.resKO(Json.obj("error" -> "No User found")))
            case Some(user) => Ok(dbHelper.resOK(Json.obj("user" -> user, "projectID" -> projectID)))
          }
        }
    }
  }

  /**
   * updates firstname, lastname and avatar of the user in the body (json) -> can only be executed by logged in users
   * if user to update == logged in user --> update possible
   * if user to update != logged in user --> check if logged in user isAdmin --> if so - update; else Response Forbidden
   */
  def updateUser() = SecuredAction(ajaxCall = true).async(parse.json) { implicit request =>
    val user = request.user.asInstanceOf[GenUser]
    val userID = request.user.identityId.userId
    val userRAW = request.body

    (userRAW \ "userid").asOpt[String] match {
      case None =>
        //No user id in post - update not possible
        Future(Ok(dbHelper.resKO(Json.obj("msg" -> "user could not be updated - there was no userid given"))))
      case Some(userIDUpdate) =>
        if (userID == userIDUpdate || user.isAdmin) { //user can update his own user and admin can update all users
          val firstname = (userRAW \ "firstname").asOpt[String].getOrElse("")
          val lastname = (userRAW \ "lastname").asOpt[String].getOrElse("")
          val avatar = (userRAW \ "avatar").asOpt[String].getOrElse("""N\A""")
          val query = Json.obj("userid" -> userIDUpdate)
          val updateQuery = Json.obj("$set" -> Json.obj("firstname" -> firstname, "lastname" -> lastname, "avatar" -> avatar))

          userDao.updateUser(query, updateQuery).map {
            case true => Ok(dbHelper.resOK(Json.obj("msg" -> "user updated")))
            case false => Ok(dbHelper.resKO(Json.obj("msg" -> "user could not be updated")))
          }
        } else {
          Future(Forbidden(Json.obj("error" -> "Not authorized"))) //non admin user can not update other users
        }
    }
  }

}