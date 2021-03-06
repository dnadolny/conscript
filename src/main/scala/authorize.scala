package conscript

import dispatch._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

object Authorize {
  def auths = :/("api.github.com").secure.POST / "authorizations"

  // rm when avaialable in released dispatch
  import com.ning.http.client.RequestBuilder
  import com.ning.http.client.Realm.{RealmBuilder,AuthScheme}
  def as_!(subject: RequestBuilder, user: String, password: String) =
    subject.setRealm(new RealmBuilder()
                     .setPrincipal(user)
                     .setPassword(password)
                     .setUsePreemptiveAuth(true)
                     .setScheme(AuthScheme.BASIC)
                     .build())

  import Conscript.http
  def apply(user: String, pass: String): Promise[Either[String, String]] =
    http(
        as_!(auths, user, pass).setBody(compact(render(
          ("note" -> "Conscript") ~
          ("note_url" -> "https://github.com/n8han/conscript") ~
          ("scopes" -> ("repo" :: Nil))
        ))) OK LiftJson.As
    ).either.left.map {
      case StatusCode(401) => "Unrecognized github login and password"
      case e => "Unexpected error: " + e.getMessage
    }.map { _.right.flatMap { js =>
      (for (JField("token", JString(token)) <- js) yield {
        Config.properties {
          _.setProperty("gh.access", token)
        }
        "Authorization stored"
      }).headOption.toRight("JSON parsing error")
    } }
}
