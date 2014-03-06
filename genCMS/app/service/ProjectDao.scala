package service

import scala.collection.mutable.Buffer
import scala.concurrent.Future

import models.DocumentTag
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.ReactiveMongoPlugin.db
import play.modules.reactivemongo.json.BSONFormats
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONStringHandler
import reactivemongo.bson.Producer.nameValue2Producer
import reactivemongo.core.commands.Aggregate
import reactivemongo.core.commands.Ascending
import reactivemongo.core.commands.Descending
import reactivemongo.core.commands.Group
import reactivemongo.core.commands.GroupField
import reactivemongo.core.commands.LastError
import reactivemongo.core.commands.Match
import reactivemongo.core.commands.Sort
import reactivemongo.core.commands.SumValue
import reactivemongo.core.commands.Unwind

object ProjectDao {
  case class AuthorRole(
    name: String,
    jobdoc: String)
  object AuthorRole {
    // Generates Writes and Reads for Feed and User thanks to Json Macros
    implicit val documenTypeConnFormat = Json.format[AuthorRole]
  }

  /* sort alphabetical and ignoring case */
  def compAlphIgnoreCaseAuthorRole(e1: AuthorRole, e2: AuthorRole) = (e1.name compareToIgnoreCase e2.name) < 0

  /** The documenttype collection */
  def collection = db.collection[JSONCollection]("projects")
  val dbHelper = DBHelper
  /**
   * Save a DocumentType.
   *
   * @return true, once saved.
   */
  def save(project: JsObject): Future[JsObject] = {
    Logger.debug("Saving Project: " + project)
    //val timestamp: Long = System.currentTimeMillis()
    //val insertDoc = documentType.copy(createdAt = timestamp, modifiedAt = timestamp)
    collection.insert(project).map {
      case ok if ok.ok =>
        project
      case error => throw new RuntimeException(error.message)
    }
  }

  /**
   * This function is used to check if the title
   * if a projectid is provided this project is excluded from search
   */
  def isTitleInUse(title: String, projectID: String = ""): Future[Boolean] = {
    var query = Json.obj("titleSearch" -> title.toLowerCase())
    if (projectID != "") {
      query = query ++ Json.obj("_id" -> Json.obj("$ne" -> Json.obj("$oid" -> projectID)))
    }
    val filter = Json.obj("_id" -> 1)
    Logger.debug("isTitleInUser Query: " + query)
    Logger.debug("isTitleInUse? " + title)
    collection.find(query, filter).cursor[JsObject].collect[List](1).map {
      projects =>
        Logger.debug("isTitleInUse projects empty?: " + projects.isEmpty)
        Logger.debug(projects.toString)
        if (projects.isEmpty)
          false
        else
          true
    }
  }

  /**
   * Select all projects available to the user from the db
   * for admin users all projects are available
   * for other users the following is true
   * 	public projects are available
   *  	projects where the user is author or projectadmin are available
   */
  def getProjects(page: Int, perPage: Int, currentUser: GenUser): Future[List[JsObject]] = {
    Logger.debug("CURRENTUSER: " + currentUser)

    val projectAdminAuthorList = (currentUser.projectAdmin match {
      case None => Nil
      case Some(pAdm) => for (pA <- pAdm) yield Json.obj("$oid" -> pA.projectID)
    }) ::: (currentUser.author match {
      case None => Nil
      case Some(pAuthor) => for (pA <- pAuthor) yield Json.obj("$oid" -> pA.projectID)
    })

    Logger.debug("ProjectAdminAuthorList")
    Logger.debug(projectAdminAuthorList.toString)

    val query = currentUser.admin match {
      case true => Json.obj()
      case false =>
        Json.obj("$or" -> Json.arr(
          Json.obj("author" -> currentUser.identityId.userId),
          Json.obj("public" -> true),
          Json.obj("_id" -> Json.obj("$in" -> JsArray(projectAdminAuthorList)))))
    }
    val filter = Json.obj("_id" -> 1, "title" -> 1, "author" -> 1, "description" -> 1)

    val f = collection.find(query, filter)
      .sort(Json.obj("modifiedAt" -> 1))
      .cursor[JsObject]

    f.collect[List](perPage).map {
      jRes => jRes
    }
  }

  def getProjectById(id: String): Future[JsObject] = {
    try {
      val f = collection.find[JsObject](dbHelper.toObjectId(id)).cursor[JsObject]
      f.collect[List](1).map {
        project =>
          if (project.isEmpty)
            null
          else
            project(0)
      }
    } catch {
      case e: NoSuchElementException => null //Wrong ID
    }
  }

  def queryProject(id: String, filter: JsObject): Future[Option[JsObject]] = {
    try {
      val f = collection.find(dbHelper.toObjectId(id), filter).cursor[JsObject]
      f.collect[List](1).map {
        project =>
          if (project.isEmpty)
            None
          else
            Some(project(0))
      }
    } catch {
      case e: NoSuchElementException => Future(None) //Wrong ID
    }
  }

  def addTag(projectID: String, tag: String): Future[(Boolean, String)] = {
    val filter = Json.obj("tags" -> 1)
    queryProject(projectID, filter).flatMap {
      case Some(project) =>
        (project \ "tags").asOpt[List[String]] match {
          case Some(tagList) =>
            tagList.indexOf(tag) match {
              case -1 => //add Tag to project
                val updateQuery = Json.obj("$set" -> Json.obj("tags" -> (tagList ::: List(tag)).sortWith(DBHelper.compAlphIgnoreCase)))
                updateProject(projectID, updateQuery).map {
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
            updateProject(projectID, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                (true, "tag was added to project")
              case LastError(false, err, code, msg, _, _, _) =>
                (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
            }
        }
      case None => Future((false, "project not found")) //update not possible
    }
  }

  def removeTag(projectID: String, tag: String): Future[(Boolean, String)] = {
    val filter = Json.obj("tags" -> 1)
    queryProject(projectID, filter).flatMap {
      case Some(project) =>
        (project \ "tags").asOpt[List[String]] match {
          case Some(tagList) =>
            //check if tag exists and has to be removed
            tagList.indexOf(tag) match {
              case -1 =>
                Future(true, "Tag is already not present at project " + projectID)
              case index => //remove Tag
                val updateQuery = Json.obj("$set" -> Json.obj("tags" -> (DBHelper.removeAt(index, tagList)._1)))
                updateProject(projectID, updateQuery).map {
                  case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                    (true, "tag was removed from project " + projectID)
                  case LastError(false, err, code, msg, _, _, _) =>
                    (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
                }
            }
          case None => Future(true, "Tag is already not present at project " + projectID)
        }
      case None => {
        Future(true, "Project not found" + projectID)
      }
    }
  }

  def addAuthorRole(projectID: String, authorRole: String): Future[(Boolean, String)] = {
    val filter = Json.obj("authorroles" -> 1)
    queryProject(projectID, filter).flatMap {
      case Some(project) =>
        (project \ "authorroles").asOpt[List[AuthorRole]] match {
          case Some(roleList) =>
            roleList indexWhere (r => r.name == authorRole) match {
              case -1 => //add AuthorRole to project
                val updateQuery = Json.obj("$set" -> Json.obj("authorroles" -> (roleList ::: List(new AuthorRole(authorRole, ""))).sortWith(compAlphIgnoreCaseAuthorRole)))
                updateProject(projectID, updateQuery).map {
                  case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                    (true, "author role was added to project")
                  case LastError(false, err, code, msg, _, _, _) =>
                    (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
                }
              case _ => //already existing author role - no update needed
                Future(true, "author role already exists")
            }
          case None => //insert as new author role
            val updateQuery = Json.obj("$set" -> Json.obj("authorroles" -> List(new AuthorRole(authorRole, ""))))
            updateProject(projectID, updateQuery).map {
              case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                (true, "author role was added to project")
              case LastError(false, err, code, msg, _, _, _) =>
                (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
            }
        }
      case None => Future((false, "project not found")) //update not possible
    }
  }

  /**
   * Removes an author role from the project only if no user exist with this role!
   * returns true, msg if successfull, false,msg if not successful
   */
  def removeAuthorRole(projectID: String, authorRole: String): Future[(Boolean, String)] = {
    val filter = Json.obj("authorroles" -> 1)
    queryProject(projectID, filter).flatMap {
      case Some(project) =>
        (project \ "authorroles").asOpt[List[AuthorRole]] match {
          case Some(roleList) =>
            //check if author role exists and has to be deleted
            roleList indexWhere (r => r.name == authorRole) match {
              case -1 =>
                Future(true, "Tag is already not present at project " + projectID)
              case index => //check if there are users left with this role!
                val userCountQuery = Json.obj("author" -> Json.obj("$elemMatch" -> Json.obj("projectID" -> projectID, "role" -> authorRole)))
                UserDao.countUsers(userCountQuery).flatMap {
                  case 0 => //delete
                    val updateQuery = Json.obj("$set" -> Json.obj("authorroles" -> (DBHelper.removeAt(index, roleList)._1)))
                    updateProject(projectID, updateQuery).map {
                      case LastError(true, _, _, _, Some(doc), updated, updatedExisting) =>
                        (true, "author role was removed from project " + projectID)
                      case LastError(false, err, code, msg, _, _, _) =>
                        (false, err.getOrElse("unknown") + " : " + msg.getOrElse("no messsage"))
                    }
                  case nrOfUsers =>
                    Future(false, "There are still " + nrOfUsers + " Users with this Role left")
                }
            }
          case None => Future(true, "Author Role is already not present at project " + projectID)
        }
      case None => {
        Future(true, "Project not found" + projectID)
      }
    }
  }

  def getProjectTitleById(id: String): Future[Option[String]] = {
    val query = dbHelper.toObjectId(id)
    val filter = Json.obj("title" -> 1)
    collection.find(query, filter).cursor[JsObject].collect[List](1).map {
      projects =>
        if (projects.isEmpty)
          None
        else
          (projects(0) \ "title").asOpt[String]
    }
  }

  /**
   * Returns the projecttitles as a JSON Object with key value pairs (projectID:Title)
   */
  def getProjectTitles(): Future[JsObject] = {
    val query = Json.obj()
    val filter = Json.obj("title" -> 1)
    val c = collection.find(query, filter).cursor[JsObject]
    c.collect[List](Integer.MAX_VALUE).map { res =>
      res.foldLeft(Json.obj())((acc, x) => acc ++ Json.obj(((x \ "_id") \ "$oid").as[String] -> (x \ "title")))
    }
  }
  /**
   * Returns the project authorroles as a JSON Object with key value pairs (projectID:[authorroles])
   */
  def getProjectAuthorRoles(): Future[JsObject] = {
    val query = Json.obj()
    val filter = Json.obj("authorroles" -> 1)
    val c = collection.find(query, filter).cursor[JsObject]
    c.collect[List](Integer.MAX_VALUE).map { res =>
      res.foldLeft(Json.obj())((acc, x) => acc ++ Json.obj(((x \ "_id") \ "$oid").as[String] -> (x \ "authorroles")))
    }
  }

  /**
   * Returns the project tags as a JSON Object with key value pairs (projectID:[tags])
   */
  def getProjectTags(): Future[JsObject] = {
    val query = Json.obj()
    val filter = Json.obj("tags" -> 1)
    val c = collection.find(query, filter).cursor[JsObject]
    c.collect[List](Integer.MAX_VALUE).map { res =>
      res.foldLeft(Json.obj())((acc, x) => acc ++ Json.obj(((x \ "_id") \ "$oid").as[String] -> (x \ "tags")))
    }
  }

  /**
   * [ "TAG1", "TAG2" ,... ]
   */
  def getDistinctProjectTags(projectID: String): Future[List[String]] = {
    db.command(Aggregate("projects", Seq(
      Match(BSONFormats.toBSON(dbHelper.toObjectId(projectID)).get.asInstanceOf[BSONDocument]),
      Unwind("tags"),
      GroupField("tags.name")(), //("count" -> SumValue(1)),
      Sort(Seq(Descending("count"), Ascending("_id")))))) map { stream =>
      val x = stream.toList.map { doc =>
        BSONFormats.toJSON(doc).asInstanceOf[JsObject]
      }
      for (tag <- x) yield (tag \ "_id").asOpt[String].getOrElse("")
    }
  }

  def getProjectTags(projectID: String): Future[List[JsObject]] = {
    db.command(Aggregate("projects", Seq(
      Match(BSONFormats.toBSON(dbHelper.toObjectId(projectID)).get.asInstanceOf[BSONDocument]),
      Unwind("tags"),
      GroupField("tags")(), //("count" -> SumValue(1)),
      Sort(Seq(Descending("count"), Ascending("_id")))))) map { stream =>
      stream.toList.map { doc =>
        BSONFormats.toJSON(doc).asInstanceOf[JsObject]
      }
    }
  }

  /**
   * [ "TAG1", "TAG2" ,... ]
   */
  def getProjectTagsStruktur(projectID: String): Future[Option[JsValue]] = {
    //def getProjectTagsStruktur(projectID: String): Future[List[JsValue]] = {
    try {
      db.command(Aggregate("projects", Seq(
        Match(BSONFormats.toBSON(dbHelper.toObjectId(projectID)).get.asInstanceOf[BSONDocument]),
        Unwind("tagsStructure"),
        Group(BSONDocument("_id" -> "$tagsStructure.name", "parent" -> "$tagsStructure.parent", "sort" -> "$tagsStructure.sort"))(), //("count" -> SumValue(1)),
        Sort(Seq(Ascending("_id.sort"), Ascending("parent")))))) map { stream =>
        val x = (
          stream.toList.map { doc =>
            BSONFormats.toJSON(doc).asInstanceOf[JsObject]
          })

        val xxx = x.foldLeft(Buffer[DocumentTag]())((acc, x) => {
          val name = ((x \ "_id") \ "_id").asOpt[String].getOrElse("")
          val sort = ((x \ "_id") \ "sort").asOpt[Int].getOrElse(0)
          ((x \ "_id") \ "parent").asOpt[String] match {
            case None => //insert as tag
              acc.indexWhere(dT => dT.name == name) match {
                case -1 => acc ++ Buffer(DocumentTag(name, sort, null))
                case index =>
                  acc(index) = DocumentTag(name, sort, acc(index).childs)
                  acc
              }
            case Some(parent) => //check if parent exists, T: include tag as child, F: include parent with child
              acc.indexWhere(dT => dT.name == parent) match {
                case -1 => acc ++ Buffer(DocumentTag(parent, -1, Buffer(DocumentTag(name, sort, null))))
                case parentIndex =>
                  val parent = acc(parentIndex)
                  parent.childs match {
                    case null =>
                      acc(parentIndex) = DocumentTag(parent.name, acc(parentIndex).order, Buffer(DocumentTag(name, sort, null)))
                      acc
                    case childs =>
                      //check if child exist
                      childs.indexWhere(dT => dT.name == name) match {
                        case -1 =>
                          acc(parentIndex) = DocumentTag(parent.name, acc(parentIndex).order, childs ++ Buffer(DocumentTag(name, sort, null)))
                          acc
                        case childIndex => acc
                      }
                  }
              }
          }
        })
        Some(Json.toJson(xxx.sortWith(DBHelper.compDocTag)))
      }
    } catch {
      case e: Throwable => Future(None) //Wrong ID
    }
  }

  def getSelectedProjectTagsWithCount(documentQuery: JsObject): Future[List[JsObject]] = {
    //val jsonQuery = Json.obj("project"->projectID, "deleted"->false, "published" -> true)
    db.command(Aggregate("documents", Seq(
      Match(BSONFormats.toBSON(documentQuery).get.asInstanceOf[BSONDocument]),
      Unwind("tags"),
      GroupField("tags")("count" -> SumValue(1)),
      Sort(Seq(Descending("count"), Ascending("_id")))))) map { stream =>
      stream.toList.map { doc =>
        BSONFormats.toJSON(doc).asInstanceOf[JsObject]
      }
    }
  }

  def getSelectedProjectConnectionsWithCount(documentQuery: JsObject): Future[List[JsObject]] = {
    //val jsonQuery = Json.obj("project"->projectID, "deleted"->false, "published" -> true)
    db.command(Aggregate("documents", Seq(
      Match(BSONFormats.toBSON(documentQuery).get.asInstanceOf[BSONDocument]),
      //Unwind("tags"),
      GroupField("connection")("count" -> SumValue(1)),
      Sort(Seq(Descending("count"), Ascending("_id")))))) map { stream =>
      stream.toList.map { doc =>
        BSONFormats.toJSON(doc).asInstanceOf[JsObject]
      }
    }
  }

  def updateProject(id: String, updateStatement: JsObject) = {
    collection.update(dbHelper.toObjectId(id), updateStatement).map {
      lastError => lastError
    }
  }

  def updateQueriedProject(query: JsObject, updateStatement: JsObject) = {
    collection.update(query, updateStatement).map {
      lastError => lastError
    }
  }

}