

package models

import reactivemongo.core.nodeset.Connection
import scala.util.parsing.json.JSON
import reactivemongo.core.commands.Count
import scala.concurrent.Await
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._

case class User(
  _id: BSONObjectID,
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  password: String)

object User {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._
  import reactivemongo.api._
  import reactivemongo.bson._
  import reactivemongo.core.commands._
  import reactivemongo.api.collections.default.BSONCollection
  import play.modules.reactivemongo.json.collection.JSONCollection
  import play.api.libs.concurrent.Execution.Implicits._
  import scala.concurrent.Future
  import scala.concurrent.duration._

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val userFormat = Json.format[User]
}
