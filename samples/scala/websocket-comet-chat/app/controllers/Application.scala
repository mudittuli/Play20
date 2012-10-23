package controllers

import play.api._
import play.api.mvc._

import play.api.libs.{ Comet }
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.json._

import models._
import scala.concurrent.{ future, promise }
import scala.concurrent.ExecutionContext.Implicits.global


object Application extends Controller {
  
  /**
   * Just display the home page.
   */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }
  
  /**
   * Display the chat room page.
   */
  def chatRoom(username: Option[String]) = Action { implicit request =>
    username.filterNot(_.isEmpty).map { username =>
      Ok(views.html.chatRoom(username))
    }.getOrElse {
      Redirect(routes.Application.index).flashing(
        "error" -> "Please choose a valid username."
      )
    }
  }
  
  /**
   * Handles the chat websocket.
   */
  def chat(username: String) = WebSocket.async[JsValue] { request  =>
    ChatRoom.join(username)
  }

  /**
   * Handles the chat comet.
   */
  def chatcomet(username: String)  = Action{
    val promiseOfEnumerator = ChatRoom.joincomet(username)
    Async {
      promiseOfEnumerator.map(enumerator => Ok.stream(enumerator &> Comet(callback = "parent.recieveData")))
    }
  }

  def postmessage = Action { request =>
    request.body.asJson.map { json => 
      try {
        ChatRoom.postmessage((json \ "username").as[String],(json \ "message").as[String])
        Ok("Hello")
        } catch {
          case ex: JsResultException => BadRequest("Not the required Json format")
        }
    }.getOrElse {
      BadRequest("Expecting Json data")
    }
  }
  
}
