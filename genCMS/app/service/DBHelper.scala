package service

import scala.math.BigDecimal.long2bigDecimal

import models.DocumentTag
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsArray
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Reads.JsObjectReducer
import play.api.libs.json.Reads.JsStringReads
import play.api.libs.json.Reads.functorReads
import play.api.libs.json.__
import reactivemongo.bson.BSONObjectID

object DBHelper {
  //(dbHelper.addCreationAndModificationDate and dbHelper.addCopyToName and dbHelper.addCreateUser(currentUser) and dbHelper.addChangeUser(currentUser) and dbHelper.addMongoId
  def transformToDocTypeCopy(docTypeNameAddition: String, user: String, time: Long) = {
    __.json.update((
      __.json.update((__ \ 'author).json.put(JsString(user)) and (__ \ 'changeAuthor).json.put(JsString(user)) reduce) andThen
      (__ \ 'name).json.update(__.read[JsString].map { s => JsString(s.value + docTypeNameAddition) }) andThen
      (__ \ 'createdAt \ '$date).json.update(__.read[JsNumber].map { n => JsNumber(time) }) andThen
      (__ \ 'modifiedAt \ '$date).json.update(__.read[JsNumber].map { n => JsNumber(time) }) andThen
      (__ \ '_id \ '$oid).json.update(__.read[JsString].map { id => JsString(BSONObjectID.generate.stringify) })))
  }

  /** Writes an ID in Json Extended Notation */
  def toObjectId(id: String): JsObject = { Json.obj("_id" -> Json.obj("$oid" -> id)) }
  val fromObjectId = (__ \ '_id).json.copyFrom((__ \ '_id \ '$oid).json.pick)
  /** Generates a new ID and adds it to your JSON using Json extended notation for BSON */
  val generateId = ((__ \ '_id \ '$oid).json.put(JsString(BSONObjectID.generate.stringify)))

  val dropId = ((__ \ 'id).json.prune)

  /** Generates a new date and adds it to your JSON using Json extended notation for BSON */
  def generateCreated(time: Long) = __.json.update((__ \ 'createdAt \ '$date).json.put(JsNumber(time)))
  def generateCreatedDocument(time: Long) = __.json.update((__ \ 'fields \ 'createdAt \ '$date).json.put(JsNumber(time)))

  /** Generates a new date and adds it to your JSON using Json extended notation for BSON */
  def generateModified(time: Long) = __.json.update((__ \ 'modifiedAt \ '$date).json.put(JsNumber(time)))
  def generateModifiedDocument(time: Long) = __.json.update((__ \ 'fields \ 'modifiedAt \ '$date).json.put(JsNumber(time)))
  //(__ \ 'modifiedAt \ '$date).json.put(JsNumber(time))

  /** set the Searchtitle lowercase */
  def setSearchTitle(value: String) = __.json.update((__ \ 'titleSearch).json.put(JsString(value.toLowerCase())))

  /** Updates Json by adding both ID and date */

  //TODO ADD DATE CREATED AND MODIFIED!!!
  val addCreationAndModificationDate: Reads[JsObject] = {
    val time = (new java.util.Date).getTime()
    __.json.update((generateCreated(time) and generateModified(time)) reduce)
  }

  val addDocumentCreationAndModificationDate: Reads[JsObject] = {
    val time = (new java.util.Date).getTime()
    __.json.update((generateCreatedDocument(time) andThen generateModifiedDocument(time)))
  }

  val addModificationDate: Reads[JsObject] = {
    val time = (new java.util.Date).getTime()
    __.json.update((generateModified(time)))
  }

  val addMongoId: Reads[JsObject] = {
    val time = (new java.util.Date).getTime()
    __.json.update(generateId)
  }

  /** Updates Json by adding both ID and date */
  val addCopyToName: Reads[JsObject] = __.json.update((__ \ 'name).json.update(
    __.read[JsString].map { s => JsString(s.value + " COPY") }))

  def addCreateUser(user: String) = __.json.update((__ \ 'author).json.put(JsString(user)))
  def addDocumentCreateUser(user: String) = __.json.update((__ \ 'fields \ 'author).json.put(JsString(user)))

  def addProjectID(projectID: String) = __.json.update((__ \ 'project).json.put(JsString(projectID)))

  def addTitle(title: String) = __.json.update((__ \ 'title).json.put(JsString(title)))

  def addDeleted(deleted: Boolean) = __.json.update((__ \ 'deleted).json.put(JsBoolean(deleted)))

  def addPublished(published: Boolean) = __.json.update((__ \ 'published).json.put(JsBoolean(published)))

  def addInEdit(inEdit: Boolean) = __.json.update((__ \ 'inEdit).json.put(JsBoolean(inEdit)))

  def addReported(reported: Boolean) = __.json.update((__ \ 'reported).json.put(JsBoolean(reported)))

  def addTags() = __.json.update((__ \ 'tags).json.put(JsArray()))

  def addLoc() = __.json.update((__ \ 'loc).json.put(JsArray()))

  def addConnectionID(connectionID: String) = __.json.update((__ \ 'connection).json.put(JsString(connectionID)))

  def addChangeUser(user: String) = __.json.update((__ \ 'changeAuthor).json.put(JsString(user)))

  def addLocale(docTypeID: String, lang: String, field: String, value: String) = __.json.update((__ \ docTypeID \ lang \ field).json.put(JsString(value)))

  val toMongoUpdate = (__ \ '$set).json.copyFrom(__.json.pick)

  /** no need to always use Json combinators or transformers, sometimes stay simple */
  def resOK(data: JsValue) = Json.obj("res" -> "OK") ++ Json.obj("data" -> data)
  def resKO(error: JsValue) = Json.obj("res" -> "KO") ++ Json.obj("error" -> error)

  def getJsonObject(list: List[JsObject]): JsObject = {
    list.foldLeft(Json.obj())((acc, x) => acc ++ x)
  }

  /**
   * Helper function used to remove a item of a list at a specified index
   */
  def removeAt[A](n: Int, ls: List[A]): (List[A], Option[A]) = ls.splitAt(n) match {
    case (Nil, _) if n < 0 => (ls, None)
    case (pre, e :: post) => (pre ::: post, Some(e))
    case (pre, Nil) => (ls, None)
  } //> removeAt: [A](n: Int, ls: List[A])(List[A], Option[A])

  /**
   * Helper function used to remove a list item
   */
  def removeListItem(item: String, list: List[String]): List[String] = {
    removeAt(list.indexOf(item), list)._1
  }

  /* sort alphabetical and ignoring case */
  def compAlphIgnoreCase(e1: String, e2: String) = (e1 compareToIgnoreCase e2) < 0

  /* sort document tags according to sort order */
  def compDocTag(e1: DocumentTag, e2: DocumentTag) = (e1.order < e2.order)

  def parseDouble(s: String) = try { Some(s.toDouble) } catch { case _: Throwable => None }
}