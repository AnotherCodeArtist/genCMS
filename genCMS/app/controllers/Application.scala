package controllers
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.Play.current
import reactivemongo.api._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import service.UserDao
import reactivemongo.bson.BSONObjectID
import scala.concurrent.duration._
import play.api.mvc.RequestHeader
import service.ProjectDao
import views.html.document.documentEdit
import securesocial.core.SecureSocial
import securesocial.core.Identity
import securesocial.core.Identity
import securesocial.core.Authorization
import service.GenUser
import securesocial.core.SecuredRequest
import play.api.i18n.Lang
import play.api.i18n.Messages
import service.MongoUserService
import service.DBHelper

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
  // -- Authentication
  //def collection: JSONCollection = db.collection[JSONCollection]("persons")

  /*def authenticateUser(email:String, password:String) = {
    if(email.equals("robin.passath@gmx.at"))
    	true
	else
		false
  }*/
  /*
  val minPasswordLength = 6
  val maxPasswordLength = 100
  val loginForm = Form(
    tuple(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText) /* verifying ("Invalid email or password", result => result match {
        case (email, password) => async{ await(UserDao.authenticate(email, password)) }
      })
      * 
      */ )

  val registerForm = Form(
    tuple(
      "username" -> nonEmptyText,
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> nonEmptyText,
      "password" -> nonEmptyText(minPasswordLength, maxPasswordLength)) /*verifying ("error at registration", result => result match {
        case (username, firstname, lastname, email, password) => Await.result(UserDao.save(User(BSONObjectID.generate, username, firstname, lastname, email, password)), 1 second) //UserDao.count(email, password)
      })
      * 
      */ )
*/
  /**
   * Login page.
   */
  /*
  def login(email: String) = Action { implicit request =>
    if (email.isEmpty())
      Ok(views.html.loginOLD(getSessionValue(request, "email"), loginForm))
    else {
      val failedForm = loginForm.fill(email, "")
      Ok(views.html.loginOLD(getSessionValue(request, "email"), failedForm))
    }
  }
*/
  /*
  def loginplain = Action { implicit request =>
    Ok(views.html.loginPlain(getSessionValue(request, "email"), loginForm))
  }
*/
  /*
   
   def register = Action { implicit request =>
    Ok(views.html.loginPlain(getSessionValue(request, "email"), null, registerForm))
  }
*/
  /**
   * Handle login form submission.
   */
  /* def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.loginOLD(getSessionValue(request, "email"), formWithErrors))),
      formSuccess => {
        val login = UserDao.authenticate(formSuccess._1, formSuccess._2)
        login.map {
          success =>
            if (success) {
              Redirect(routes.Application.index).withSession("email" -> formSuccess._1).withCookies(Cookie("locale", "en", httpOnly = false))
            } else {
              val failedForm = loginForm.fillAndValidate(formSuccess._1, "") //.withError("login", "username or password", "email")
              Logger.debug(failedForm.toString)
              Ok(views.html.loginOLD("", failedForm, null, Map("error" -> "Username or Password is incorrect"))) //.flashing("error"->"Username or Password is incorrect")
              //Redirect(routes.Application.login).flashing("error" -> "Username or Password is incorrect")
            }
        }
      })
    //user => Redirect(routes.Application.index).withSession("email" -> user._1))
  }
*/
  /*      
  def sendRegistration = Action.async { implicit request =>
    registerForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.loginOLD(getSessionValue(request, "email"), null, formWithErrors))),
      formSuccess => {
        val registration = UserDao.save(User(BSONObjectID.generate, formSuccess._1, formSuccess._2, formSuccess._3, formSuccess._4, formSuccess._5))
        registration.map {
          success =>
            if (success) {
              Redirect(routes.Application.login).flashing("success" -> "registration successfull!")
            } else {
              val failedForm = registerForm.fill(formSuccess._1, formSuccess._2, formSuccess._3, formSuccess._4, "")
              BadRequest(views.html.loginOLD(getSessionValue(request, "email"), null, registerForm)).flashing("error" -> "registration not successfull")
            }
        }
      })
    //user => Redirect(routes.Application.index).flashing("success" -> "registration successfull!"))
  }
*/
  /**
   * Logout and clean the session.
   */
  /* def logout = Action {
    Redirect(routes.Application.index).withNewSession.flashing(
      "success" -> "You've been logged out")
  }
*/
  /**
   * Provide security features
   */
  /* trait Secured {
    /**
     * Retrieve the connected user email.
     */
    private def username(request: RequestHeader) = request.session.get("email")

  }
  * */

  def changeLanguage(langCode: String) = Action { implicit request =>
    implicit val lang = langCode match {
      case "de" => Lang("de")
      case "en" => Lang("en")
      case "fr" => Lang("fr")
      case "es" => Lang("es")
      case _ => Lang("en")
    }
    Ok(Messages("hello.world")).withLang(lang)
    //Redirect("/", 301).withLang(lang)
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

  /* def sendMail() = Action {
    /*
     
    val email = "robinpassath@hotmail.com"

    val body: Body = new Body(views.txt.email.body.render().toString(),
      views.html.email.body.render().toString());
    Mailer.getDefaultMailer().sendMail("play-easymail | it works!",
      body, email);

    //flash("message", "Mail to '" + email
    //		+ "' has been sent successfully!");
     *  
     */
    Redirect(routes.Application.index).flashing("message" -> "The email has been send") //"message","Mail to '" + email+ "' has been sent successfully!")
  }
*/
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

  def getNotFoundPage = UserAwareAction { implicit request =>
    Ok(views.html.page404())
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
        Ok(views.html.notFoundPage(request.path))
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
        Ok(views.html.notFoundPage(request.path))
      }
    }
  }

  def getDocumentEditor = SecuredAction(ajaxCall = true) { implicit request =>
    request.session.get("project") match {
      case Some(projectID) => {
        Ok(views.html.document.documentEdit())
      }
      case None => {
        Ok(views.html.notFoundPage(request.path))
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