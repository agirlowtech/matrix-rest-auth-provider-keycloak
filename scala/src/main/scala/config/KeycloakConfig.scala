package config

import sttp.client._
import zio._

import scala.util.Try

case class KeycloakConfig(
  serverPort: Int,
  baseUrl: String,
  realm: String,
  clientId: String,
  clientSecret: String,
  matrixDomain: String
) {

  private[this] val baseUrlOpenId = s"${baseUrl}/auth/realms/$realm/protocol/openid-connect"

  val openIdUri: sttp.model.Uri = uri"$baseUrlOpenId/token"
  val userInfoUri : sttp.model.Uri = uri"$baseUrlOpenId/userinfo"
}

object KeycloakConfig {

  class MissingEnvironmentVariableException(msg: String) extends Exception {
    override def getMessage: String = msg
  }

  private def getEnvVarOrRaiseMissing(variable: String): ZIO[system.System, Exception, String] = {
    system.env(variable) flatMap {
      case Some(v) =>
        ZIO.fromFunctionM(_ => ZIO.succeed(v))
      case None =>
        ZIO.fromFunctionM(_ => ZIO.fail(new MissingEnvironmentVariableException(s"Environment variable $variable is missing")))
    }
  }

  def fromSystem(): ZIO[system.System, Exception, KeycloakConfig] = {
    for {
      pString <- getEnvVarOrRaiseMissing("SERVER_PORT")
      pInt    <- ZIO.fromFunctionM[system.System, Exception, Int](_ =>
                  IO
                    .fromTry(Try(pString.toInt))
                    .mapError(e => new Exception(e.getMessage))
                  )
      b       <- getEnvVarOrRaiseMissing("KEYCLOAK_BASE_URL")
      r       <- getEnvVarOrRaiseMissing("KEYCLOAK_REALM")
      cId     <- getEnvVarOrRaiseMissing("KEYCLOAK_CLIENT_ID")
      cSecret <- getEnvVarOrRaiseMissing("KEYCLOAK_CLIENT_SECRET")
      mxd     <- getEnvVarOrRaiseMissing("MATRIX_DOMAIN")
    } yield KeycloakConfig(pInt, b, r, cId, cSecret, mxd)

  }
}