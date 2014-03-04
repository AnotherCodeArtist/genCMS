package controllers.plugin

import play.api.mvc.{RequestHeader, Request}
import play.api.templates.{Txt,Html}
import securesocial.core.{Identity, SecuredRequest, SocialUser}
import play.api.data.Form
import securesocial.controllers.Registration.RegistrationInfo
import securesocial.controllers.PasswordChange.ChangeInfo
import securesocial.controllers.TemplatesPlugin
import securesocial.core.SecureSocial
import play.api.i18n.Lang
/**
 * Created with IntelliJ IDEA.
 * User: shrikar
 * Date: 10/10/13
 * Time: 7:51 PM
 * To change this template use File | Settings | File Templates.
 */
class MyViews(application: play.Application) extends TemplatesPlugin
{
  implicit def user(implicit request: RequestHeader):Option[Identity] = {
    SecureSocial.currentUser
  }
  
  override def getLoginPage[A](implicit request: Request[A], form: Form[(String, String)],
                               msg: Option[String] = None): Html =
  {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    views.html.login(form, msg)
  }

  override def getSignUpPage[A](implicit request: Request[A], form: Form[RegistrationInfo], token: String): Html = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    views.html.Registration.signUp(form, token)
  }

  override def getStartSignUpPage[A](implicit request: Request[A], form: Form[String]): Html = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    views.html.Registration.startSignUp(form)
  }

  override def getStartResetPasswordPage[A](implicit request: Request[A], form: Form[String]): Html = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    views.html.Registration.startResetPassword(form)
  }

  def getResetPasswordPage[A](implicit request: Request[A], form: Form[(String, String)], token: String): Html = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    views.html.Registration.resetPasswordPage(form, token)
  }

  def getPasswordChangePage[A](implicit request: SecuredRequest[A], form: Form[ChangeInfo]):Html = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    views.html.passwordChange(form)
  }

  def getNotAuthorizedPage[A](implicit request: Request[A]): Html = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    views.html.notAuthorized()
  }

  def getSignUpEmail(token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    (None, Some(views.html.mails.signUpEmail(token)))
  }

  def getAlreadyRegisteredEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    (None, Some(views.html.mails.alreadyRegisteredEmail(user)))
  }

  def getWelcomeEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    (None, Some(views.html.mails.welcomeEmail(user)))
  }

  def getUnknownEmailNotice()(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    (None, Some(views.html.mails.unknownEmailNotice(request)))
  }

  def getSendPasswordResetEmail(user: Identity, token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    (None, Some(views.html.mails.passwordResetEmail(user, token)))
  }

  def getPasswordChangedNoticeEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    implicit val lang = Lang(request.cookies.get("PLAY_LANG") match {case Some(cookie)=> cookie.value; case None => "en"})
    (None, Some(views.html.mails.passwordChangedNotice(user)))
  }
}