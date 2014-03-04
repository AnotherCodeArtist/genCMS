package service

import _root_.java.util.Date
import securesocial.core._
import play.api.{ Logger, Application }
import securesocial.core.providers.Token
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import securesocial.core.IdentityId
import securesocial.core.providers.Token
import play.modules.reactivemongo.MongoController
import play.api.mvc.Controller
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Await
import scala.concurrent.duration._
import reactivemongo.core.commands.GetLastError
import scala.util.parsing.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.format.{ DateTimeFormatter, DateTimeFormat }
import scala.concurrent.Future
import reactivemongo.core.commands._
import play.modules.reactivemongo.json.BSONFormats._
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson.BSONDocument
import reactivemongo.api.QueryOpts

/**
 * Created with IntelliJ IDEA.
 * User: shrikar
 * Date: 9/29/13
 * Time: 2:22 PM
 * To change this template use File | Settings | File Templates.
 */
case class GenUser(
  identityId: IdentityId,
  firstName: String,
  lastName: String,
  fullName: String,
  email: Option[String],
  avatarUrl: Option[String],
  authMethod: AuthenticationMethod,
  oAuth1Info: Option[OAuth1Info],
  oAuth2Info: Option[OAuth2Info],
  passwordInfo: Option[PasswordInfo],
  selectedProject: Option[String],
  admin: Boolean = false,
  author: Option[List[AuthorConnection]] = None, //admin, projectadmin, autor
  projectAdmin: Option[List[AdminConnection]] = None) extends Identity {

  //admin will always be projectadmin
  def isProjectAdmin(project: String): Boolean = {
    admin match {
      case true => true
      case false =>
        projectAdmin match {
          case Some(list) => {
            (list indexWhere (p => p.projectID == project)) != -1
          }
          case None => false
        }
    }
  }
  //projectadmin will always be author (--> admin will also be always author)
  def isProjectAuthor(project: String): Boolean = {
    isProjectAdmin(project) match {
      case true => true
      case false =>
        author match {
          case Some(list) => {
            (list indexWhere (p => p.projectID == project)) != -1
          }
          case None => false
        }
    }
  }

  def isAdmin(): Boolean = {
    admin
  }

  def getSelectedProject(): Option[String] = {
    selectedProject
  }
  
}

object GenUser {
  def apply(i: Identity): GenUser = {
    GenUser(
      i.identityId, i.firstName, i.lastName, i.fullName,
      i.email, i.avatarUrl, i.authMethod, i.oAuth1Info,
      i.oAuth2Info, i.passwordInfo, None, false, None, None)
  }

}

case class AuthorConnection(
  projectID: String,
  role: String = "default")

object AuthorConnection {
  implicit val authorConnectionFormat = Json.format[AuthorConnection]
}

case class AdminConnection(
  projectID: String)
object AdminConnection {
  implicit val authorConnectionFormat = Json.format[AdminConnection]
}

class MongoUserService(application: Application) extends UserServicePlugin(application) with Controller with MongoController {
  def collection: JSONCollection = db.collection[JSONCollection]("users")
  def tokens: JSONCollection = db.collection[JSONCollection]("tokens")

  private var tokens1 = Map[String, Token]()
  val outPutUser = (__ \ "id").json.prune

  def retIdentity(json: JsObject): Identity = {
    val userid = (json \ "userid").as[String]

    val provider = (json \ "provider").as[String]
    val firstname = (json \ "firstname").as[String]
    val lastname = (json \ "lastname").as[String]
    val email = (json \ "email").as[String]
    val avatar = (json \ "avatar").as[String]
    val hash = (json \ "password" \ "hasher").as[String]
    val password = (json \ "password" \ "password").as[String]
    println("password : " + password)
    val salt = (json \ "password" \ "salt").asOpt[String]
    val authmethod = (json \ "authmethod").as[String]

    val identity: IdentityId = new IdentityId(userid, authmethod)
    val authMethod: AuthenticationMethod = new AuthenticationMethod(authmethod)
    val pwdInfo: PasswordInfo = new PasswordInfo(hash, password)

    val selectedProject = (json \ "selectedProject").asOpt[String]
    val admin = (json \ "admin").as[Boolean]
    val author: Option[List[AuthorConnection]] = (json \ "author").asOpt[List[AuthorConnection]]
    val projectadmin: Option[List[AdminConnection]] = (json \ "projectadmin").asOpt[List[AdminConnection]]

    val user: GenUser = new GenUser(identity, firstname, lastname, firstname, Some(email), Some(avatar), authMethod, None, None, Some(pwdInfo), selectedProject, admin, author, projectadmin)
    user
  }

  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = {

    Logger.debug("MongoUserService: findByEmailAndProvider: " + email + " / " + providerId)
    val provider = providerId match {
      case "userPassword" => "userpass"
      case "userpass" => "userpass"
      case _ => providerId
    }
    val cursor = collection.find(Json.obj("userid" -> email, "provider" -> provider)).cursor[JsObject]
    val futureuser = cursor.headOption.map {
      case Some(user) => user
      case None => false
    }
    val jobj = Await.result(futureuser, 5 seconds)
    jobj match {
      case x: Boolean => None
      case _ => Some(retIdentity(jobj.asInstanceOf[JsObject]))
    }
  }

  def findByEmail(email: String): Future[Option[Identity]] = {
    val cursor = collection.find(Json.obj("userid" -> email, "provider" -> "userpass")).cursor[JsObject]
    val futureuser = cursor.headOption

    futureuser.map {
      case Some(jobj) => Some(retIdentity(jobj.asInstanceOf[JsObject]))
      case None => None
    }
  }

  

  def updateUser(query: JsObject, updateQuery: JsObject, upsert: Boolean = false) = {
    collection.update(query, updateQuery, GetLastError(), upsert)
      .map {
        case LastError(true, _, _, _, Some(doc), updated, updatedExisting) => {
          updated match {
            case 1 => {
              true
            }
            case _ => false
          }
        }
        case LastError(false, err, code, msg, _, _, _) => false
      }
  }

  def save(user: Identity): Identity = {
    //check if user exists in db - if so: update, else insert
    Logger.debug("MongoUserService: save user: " + user)
    val provider = user.authMethod.method match {
      case "userPassword" => "userpass"
      case _ => user.identityId.providerId
    }
    val query = Json.obj("userid" -> user.identityId.userId, "provider" -> provider)
    val email = user.email match {
      case Some(email) => email
      case _ => "N/A"
    }

    val avatar = user.avatarUrl match {
      case Some(url) => url
      case _ => "N/A"
    }

    checkIfUserIsStored(user.identityId.userId, provider) match {
      case true => //update
        val updateJson = Json.obj("$set" -> Json.obj(
          "firstname" -> user.firstName,
          "lastname" -> user.lastName,
          "email" -> email,
          "avatar" -> avatar,
          "authmethod" -> user.authMethod.method,
          "password" -> Json.obj("hasher" -> user.passwordInfo.get.hasher, "password" -> user.passwordInfo.get.password, "salt" -> user.passwordInfo.get.salt),
          "lastLogin" -> Json.obj("$date" -> new Date())))
        Logger.debug("Update User: " + Json.prettyPrint(updateJson))
        updateUser(query, updateJson, upsert = false)
      case false => //insert
        val saveJson = Json.obj(
          "userid" -> user.identityId.userId,
          "provider" -> provider,
          "firstname" -> user.firstName,
          "lastname" -> user.lastName,
          "email" -> email,
          "avatar" -> avatar,
          "authmethod" -> user.authMethod.method,
          "password" -> Json.obj("hasher" -> user.passwordInfo.get.hasher, "password" -> user.passwordInfo.get.password, "salt" -> user.passwordInfo.get.salt),
          "createdAt" -> Json.obj("$date" -> new Date()),
          "lastLogin" -> Json.obj("$date" -> new Date()),
          "admin" -> false,
          "author" -> JsArray(),
          "projectadmin" -> JsArray())
        Logger.debug("Create User: " + Json.prettyPrint(saveJson))
        //insert new user
        collection.insert(saveJson)
    }

    user
  }

  def checkIfUserIsStored(email: String, providerId: String): Boolean = {
    val cursor = collection.find(Json.obj("userid" -> email, "provider" -> providerId), Json.obj("userid" -> 1)).cursor[JsObject]
    val futureuser = cursor.headOption.map {
      case Some(user) => user
      case None => false
    }
    val jobj = Await.result(futureuser, 5 seconds)
    jobj match {
      case x: Boolean => false
      case _ => true
    }
  }

  def find(id: IdentityId): Option[Identity] = {
    findByEmailAndProvider(id.userId, id.providerId)
  }

  def save(token: Token) {
    tokens1 += (token.uuid -> token)
    val tokentosave = Json.obj(
      "uuid" -> token.uuid,
      "email" -> token.email,
      "creation_time" -> Json.obj("$date" -> token.creationTime),
      "expiration_time" -> Json.obj("$date" -> token.expirationTime),
      "isSignUp" -> token.isSignUp)
    tokens.save(tokentosave)
  }

  def findToken(token: String): Option[Token] = {

    val cursor = tokens.find(Json.obj("uuid" -> token)).cursor[JsObject]
    val futureuser = cursor.headOption.map {
      case Some(user) => user
      case None => false
    }
    val jobj = Await.result(futureuser, 5 seconds)
    jobj match {
      case x: Boolean => None
      case obj: JsObject => {
        println(obj)
        val uuid = (obj \ "uuid").as[String]
        val email = (obj \ "email").as[String]
        val created = (obj \ "creation_time" \ "$date").as[Long]
        val expire = (obj \ "expiration_time" \ "$date").as[Long]
        val signup = (obj \ "isSignUp").as[Boolean]
        val df = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
        Some(new Token(uuid, email, new DateTime(created), new DateTime(expire), signup))
      }
    }
  }

  def deleteToken(uuid: String) {
    Logger.debug("MongoUserService: deleteToken: " + uuid)
    val query = Json.obj("uuid" -> uuid)
    tokens.remove(query, firstMatchOnly = true)
  }

  def deleteExpiredTokens() {
    Logger.debug("MongoUserService: deleteExpiredTokens: " + new Date())
    val query = Json.obj("expiration_time" -> Json.obj("$lt" -> Json.obj("$date" -> new Date())))
    tokens.remove(query, firstMatchOnly = false)
  }
  
}
