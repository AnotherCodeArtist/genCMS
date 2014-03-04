import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future
import controllers.Application

object Global extends GlobalSettings {
  /*override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound(
      views.html.notFoundPage(request.path, Application.getSessionValue(request, "email"))
      ))
  }*/
}