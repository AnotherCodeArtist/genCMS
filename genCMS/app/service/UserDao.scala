package service

import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import models.User
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
import securesocial.core.Identity
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import securesocial.core.IdentityId
import securesocial.core.AuthenticationMethod
import securesocial.core.PasswordInfo
import play.modules.reactivemongo.json.BSONFormats

object UserDao {
  /** The user collection */
  private def collection = db.collection[JSONCollection]("users")

  def findByEmail(email: String): Future[Option[Identity]] = {
    val cursor = collection.find(Json.obj("userid" -> email, "provider" -> "userpass")).cursor[JsObject]
    val futureuser = cursor.headOption

    futureuser.map {
      case Some(jobj) => Some(retIdentity(jobj.asInstanceOf[JsObject]))
      case None => None
    }
  }

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

  def makeAdmin(email: String): Future[(Boolean, String)] = {
    findByEmail(email: String).flatMap {
      case Some(user) => {
        val admin = user.asInstanceOf[GenUser].admin
        if (admin)
          Future((true, "user is already admin"))
        else {
          val query = Json.obj("userid" -> email)
          val updateQuery = Json.obj("$set" -> Json.obj("admin" -> true))
          updateUser(query, updateQuery, false).map {
            case true => (true, "user is admin now")
            case false => (false, "user update failed")
          }
        }
      }
      case None => Future((false, "user not found")) //update not possible
    }
  }

  def removeAdmin(email: String): Future[(Boolean, String)] = {
    findByEmail(email: String).flatMap {
      case Some(user) => {
        val admin = user.asInstanceOf[GenUser].admin
        if (admin) {
          val query = Json.obj("userid" -> email)
          val updateQuery = Json.obj("$set" -> Json.obj("admin" -> false))
          updateUser(query, updateQuery, false).map {
            case true => (true, "user is no admin now")
            case false => (false, "user update failed")
          }
        } else {
          Future((true, "user is no admin"))
        }
      }
      case None => Future((false, "user not found")) //update not possible
    }
  }

  def makeProjectAdmin(email: String, projectID: String): Future[(Boolean, String)] = {
    findByEmail(email: String).flatMap {
      case Some(user) => {
        user.asInstanceOf[GenUser].projectAdmin match {
          case Some(pAList) => {
            //check if already projectAdmin -> return existing List, else add projectID
            val pac = AdminConnection(projectID)
            val indexOfCon = pAList indexWhere (p => p.projectID == projectID)

            if (indexOfCon != -1)
              Future(true, "User is already admin for project " + projectID)
            else {
              val query = Json.obj("userid" -> email)
              val updateQuery = Json.obj("$set" -> Json.obj("projectadmin" -> (pAList ++ List(pac))))
              updateUser(query, updateQuery, false).map {
                case true => (true, "user is now project admin for: " + projectID)
                case false => (false, "user update failed")
              }
            }
          }
          case None => {
            val query = Json.obj("userid" -> email)
            val updateQuery = Json.obj("$set" -> Json.obj("projectadmin" -> (List(AdminConnection(projectID)))))
            updateUser(query, updateQuery, false).map {
              case true => (true, "user is now project admin for: " + projectID)
              case false => (false, "user update failed")
            }
          }
        }
      }
      case None => Future((false, "user not found")) //update not possible
    }
  }

  def removeProjectAdmin(email: String, projectID: String): Future[(Boolean, String)] = {
    findByEmail(email: String).flatMap {
      case Some(user) => {
        user.asInstanceOf[GenUser].projectAdmin match {
          case Some(pAList) => {
            //check if already projectAdmin -> return existing List, else add projectID
            val indexOfCon = pAList indexWhere (p => p.projectID == projectID)
            val pac = AdminConnection(projectID)

            if (indexOfCon != -1) { //user is admin for project -> remove connection
              val query = Json.obj("userid" -> email)
              val updateQuery = Json.obj("$set" -> Json.obj("projectadmin" -> (DBHelper.removeAt(indexOfCon, pAList)._1)))
              updateUser(query, updateQuery, false).map {
                case true => (true, "user was removed from project admins for project: " + projectID)
                case false => (false, "user update failed")
              }
            } else {
              Future(true, "User is already no project admin for project " + projectID)
            }
          }
          case None => {
            Future(true, "User is already no project admin for project " + projectID)
          }
        }
      }
      case None => Future((false, "user not found")) //update not possible
    }
  }

  def makeProjectAuthor(email: String, projectID: String, role: String = "default"): Future[(Boolean, String)] = {
    findByEmail(email: String).flatMap {
      case Some(user) => {
        user.asInstanceOf[GenUser].author match {
          case Some(authorList) => {
            //check if already projectAdmin -> return existing List, else add projectID
            val authorC = AuthorConnection(projectID, role)
            val indexOfCon = authorList indexWhere (p => p.projectID == projectID)

            if (indexOfCon != -1) { //already some author - check if role is the given one
              val con = authorList(indexOfCon)
              if (con.role == role)
                Future(true, "User is already author for project " + projectID + " with role: " + role)
              else {
                val query = Json.obj("userid" -> email)
                val updateQuery = Json.obj("$set" -> Json.obj("author" -> ((DBHelper.removeAt(indexOfCon, authorList)._1 ++ List(authorC)))))
                updateUser(query, updateQuery, false).map {
                  case true => (true, "User is now author for project " + projectID + " with role: " + role)
                  case false => (false, "user update failed")
                }
              }
            } else { //not an author yet -> add
              val query = Json.obj("userid" -> email)
              val updateQuery = Json.obj("$set" -> Json.obj("author" -> (authorList ++ List(authorC))))
              updateUser(query, updateQuery, false).map {
                case true => (true, "user is now project admin for: " + projectID)
                case false => (false, "user update failed")
              }
            }
          }
          case None => {
            val query = Json.obj("userid" -> email)
            val updateQuery = Json.obj("$set" -> Json.obj("author" -> (List(AuthorConnection(projectID, role)))))
            updateUser(query, updateQuery, false).map {
              case true => (true, "User is now author for project " + projectID + " with role: " + role)
              case false => (false, "user update failed")
            }
          }
        }
      }
      case None => Future((false, "user not found")) //update not possible
    }
  }

  def removeProjectAuthor(email: String, projectID: String): Future[(Boolean, String)] = {
    findByEmail(email: String).flatMap {
      case Some(user) => {
        user.asInstanceOf[GenUser].author match {
          case Some(authorList) => {
            //check if already projectAdmin -> return existing List, else add projectID
            val indexOfCon = authorList indexWhere (p => p.projectID == projectID)

            if (indexOfCon != -1) { //user is admin for project -> remove connection
              val query = Json.obj("userid" -> email)
              val updateQuery = Json.obj("$set" -> Json.obj("author" -> (DBHelper.removeAt(indexOfCon, authorList)._1)))
              updateUser(query, updateQuery, false).map {
                case true => (true, "user is removed from authors for project: " + projectID)
                case false => (false, "user update failed")
              }
            } else {
              Future(true, "User is already no author for project " + projectID)
            }
          }
          case None => {
            Future(true, "User is already no author for project " + projectID)
          }
        }
      }
      case None => Future((false, "user not found")) //update not possible
    }
  }

  def countUsers(query: JsObject): Future[Int] = {
    db.command(Count(collection.name, Some(BSONFormats.toBSON(query).get.asInstanceOf[BSONDocument])))
  }

  /**
   * Return all users (with pagination), the total count of users matching the query (for pagination) and the current project id
   */
  def getUsers(page: Int, perPage: Int, projectID: String, projectOnly: Boolean = true, filteredOnly: Boolean = false, admin: Boolean = false, projectAdmin: Boolean = false, author: Boolean = false, orderBy: String = "userid", asc: Boolean = true, firstName: Option[String], lastName: Option[String], userID: Option[String]): Future[JsObject] = {
    var query = Json.obj()

    if (filteredOnly) {
      if (admin) { //Apply filters (admin, projectAdmin, author, author role) only if checked
        query = query ++ Json.obj("admin" -> true) //, "published" -> published)
      } else if (projectAdmin) {
        projectOnly match {
          case true => query = query ++ Json.obj("projectadmin" -> Json.obj("$elemMatch" -> Json.obj("projectID" -> projectID)))
          case false => query = query ++ Json.obj("projectadmin" -> Json.obj("$not" -> Json.obj("$size" -> 0))) //at least some project admin
        }
      } else if (author) {
        projectOnly match {
          case true => query = query ++ Json.obj("author" -> Json.obj("$elemMatch" -> Json.obj("projectID" -> projectID)))
          case false => query = query ++ Json.obj("author" -> Json.obj("$not" -> Json.obj("$size" -> 0))) //at least some author
        }
      }
    }

    if (projectOnly && !filteredOnly) { //get all users somehow connected to project
      query = query ++ Json.obj("$or" -> Json.arr(
        Json.obj("admin" -> true),
        Json.obj("projectadmin" -> Json.obj("$elemMatch" -> Json.obj("projectID" -> projectID))),
        Json.obj("author" -> Json.obj("$elemMatch" -> Json.obj("projectID" -> projectID)))))
    }

    firstName match {
      case Some(firstN) => query = query ++ Json.obj("firstname" -> Json.obj("$regex" -> firstN))
    }

    lastName match {
      case Some(lastN) => query = query ++ Json.obj("lastname" -> Json.obj("$regex" -> lastN))
    }

    userID match {
      case Some(userid) => query = query ++ Json.obj("userid" -> Json.obj("$regex" -> userid))
    }

    val filter = Json.obj("userid" -> 1, "projectadmin" -> 1, "firstname" -> 1, "lastname" -> 1, "email" -> 1, "createdAt" -> 1, "lastLogin" -> 1, "author" -> 1, "admin" -> 1)

    val sort = Json.obj(orderBy -> (if (asc) 1 else -1))

    countUsers(query).flatMap { totalCount =>
      val f = collection.find(query, filter)
        .options(QueryOpts(skipN = page * perPage))
        .sort(sort)
        .cursor[JsObject]
      f.collect[List](perPage).map {
        jRes => Json.obj("results" -> totalCount) ++ Json.obj("users" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectID)
        //jRes => Ok(dbHelper.resOK(Json.obj("results" -> totalCount) ++ Json.obj("documents" -> Json.toJson(jRes)) ++ Json.obj("projectID" -> projectId)))
      }
    }
  }
  
  /**
   * returns the data of a single user
   */
  def getUser(email:String): Future[Option[JsObject]] = {
    val query = Json.obj("userid" -> email, "provider" -> "userpass")
    val filter = Json.obj("avatar"->1, "userid" -> 1, "projectadmin" -> 1, "firstname" -> 1, "lastname" -> 1, "email" -> 1, "createdAt" -> 1, "lastLogin" -> 1, "author" -> 1, "admin" -> 1)
    val cursor = collection.find(query, filter).cursor[JsObject]
    val futureuser = cursor.headOption

    futureuser.map {
      case Some(jobj) => Some(jobj)
      case None => None
    }
  }
  
  /**
   * updates a user selected by the parameter query
   * @updateQuery $set -> .... the fields/values to update
   * @upsert	true -> insert if not existing, false -> no insert if not existing 
   */
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

  /**
   * Save a message.
   *
   * @return The saved message, once saved.
   */
  /* def save(user: User): Future[Boolean] = {
    Logger.debug("Saving User " + user.email)
    collection.save(user).map {
      case ok if ok.ok =>
        true
      case error => throw new RuntimeException(error.message)
    }
  }

  def authenticate(email: String, password: String): Future[Boolean] = {
    //Check if user with this credentials exists
    Logger.debug("checking user " + email)
    val query = BSONDocument("email" -> email, "password" -> password)
    val futureCount = db.command(Count(collection.name, Some(query))) // sort them by creation date
    futureCount.map {
      count =>
        if (count > 0)
          true
        else
          false
    }

  }

*
* 
*/
  /*
    /** The total number of messages */
  def count: Future[Int] = {
    ReactiveMongoPlugin.db.command(Count(collection.name))
  }
*/
  /* def createUser() = Action{
    val user = User("seppi","sepp@test.at", "Sepp", "Bucher","password")
     // insert the user
    val futureResult = collection.insert(user)
    Async {
      // when the insert is performed, send a OK 200 result
      futureResult.map(_ => Ok)
    }		
  }
  
  def findUsers(name:String) = Action{
    Async {
      val cursor: Cursor[User] = collection.
        // find all people with name `name`
        find(Json.obj("firstName" -> name)).
        // sort them by creation date
        sort(Json.obj("created" -> -1)).
        // perform the query and get a cursor of JsObject
        cursor[User]

      // gather all the JsObjects in a list
      val futureUsersList: Future[List[User]] = cursor.collect[List](10, true)

      // everything's ok! Let's reply with the array
      futureUsersList.map { persons =>
        Ok(persons.toString)
      }
    }
  }
  */
  /*
  /**
   * Find all the messages.
   *
   * @param page The page to retrieve, 0 based.
   * @param perPage The number of results per page.
   * @return All of the messages.
   */
  def findAll(page: Int, perPage: Int): Future[Seq[Message]] = {
    collection.find(Json.obj())
      .options(QueryOpts(skipN = page * perPage))
      .sort(Json.obj("_id" -> -1))
      .cursor[Message]
      .toList(perPage)
  }



  
  
  
  
  
  * */

}