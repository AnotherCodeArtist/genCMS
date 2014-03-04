package models

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

import reactivemongo.bson._

import anorm._
import anorm.SqlParser._

import scala.language.postfixOps

case class UserOld(email: String, name: String, password: String)

object UserOld {
  
  // -- Parsers
  
  /**
   * Parse a User from a ResultSet
   */
  val simple = {
    get[String]("user.email") ~
    get[String]("user.name") ~
    get[String]("user.password") map {
      case email~name~password => UserOld(email, name, password)
    }
  }
  
  // -- Queries
  
  /**
   * Retrieve a User from email.
   */
  def findByEmail(email: String): Option[UserOld] = {
    Option.apply(new UserOld("robin.passath@gmx.at", "Robin", "password"))
  }
  
  /**
   * Retrieve all users.
   */
  /*def findAll: Seq[User] = {
    DB.withConnection { implicit connection =>
      SQL("select * from user").as(User.simple *)
    }
  }*/
  
  /**
   * Authenticate a User.
   */
  
  def authenticate(email: String, password: String): Option[UserOld] = {
    if(email.equals("robin.passath@gmx.at"))
    	Option.apply(new UserOld("robin.passath@gmx.at", "Robin", "password"))
	else
		Option.empty
  }
   
  /**
   * Create a User.
   */
  /*def create(user: User): User = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into user values (
            {email}, {name}, {password}
          )
        """
      ).on(
        'email -> user.email,
        'name -> user.name,
        'password -> user.password
      ).executeUpdate()
      
      user
      
    }
  }*/
  
}






