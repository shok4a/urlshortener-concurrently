package controllers

import controllers.Helper._
import play.api.Play
import play.api.Play.current
import play.api.data.Forms._
import play.api.data._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Properties
import scalaj.http.{Http, HttpOptions}

class Application extends Controller {

  val token: String = Play.current.configuration.getString("application.token").getOrElse("")
  val form: Form[String] = Form(Forms.single("arg" -> text))

  def index = Action {
    Ok(views.html.index("", ""))
  }

  def create = Action.async(parse.urlFormEncoded) { implicit request =>
    (for {
      arg <- form.bindFromRequest().toEither.leftMap(x => Future(Ok(views.html.index(x.value.getOrElse(""), ""))))
    } yield {
      val input = arg.split(Properties.lineSeparator).map(_.trim)
        .map(x => Future(Application.urlShortener(x, token)) fallbackTo Future("エラー")).toList
      Future.sequence(input).map(_.mkString(Properties.lineSeparator)).map(x => Ok(views.html.index(arg, x)))
    }).merge
  }
}

object Application {
  def urlShortener(url: String, token: String): String = {
    val result = Http(s"https://www.googleapis.com/urlshortener/v1/url?key=$token")
      .postData(Json.stringify(Json.obj("longUrl" -> url)))
      .header(HeaderNames.CONTENT_TYPE, MimeTypes.JSON)
      .option(HttpOptions.connTimeout(10000))
      .option(HttpOptions.readTimeout(50000))
    (Json.parse(result.asString.body) \ "id").as[String]
  }
}
