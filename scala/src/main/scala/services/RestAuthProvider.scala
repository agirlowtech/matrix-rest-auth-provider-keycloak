package services

import config.KeycloakConfig
import data._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import org.http4s._
import services.KeycloakOIDClient._
import zio._
import zio.interop.catz._

object RestAuthProvider {
  type Env = Logging with Has[KeycloakConfig]
}

case class RestAuthProvider(kClient: KeycloakOIDClient.Service, kConfig: KeycloakConfig, log: Logging.Service) {

  object ioz extends Http4sDsl[Task]
  import ioz._

  val pingService: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root => Ok("pong !")
  }

  def failure(err: String): Task[Response[Task]] =
    for {
      _ <- log.error(err)
      r <- Ok(s"""|{
                  |  "auth": {
                  |    "success": false,
                  |    "reason": "$err"
                  |  }
                  |}
                  |""".stripMargin)
    } yield r

  def success(mxid: String, displayName: String): Task[Response[Task]] =
    for {
      _ <- log.info(s"authentication successful for $mxid")
      r <- Ok(s"""|{
                  |  "auth": {
                  |    "success": true,
                  |    "mxid": "${mxid},
                  |    "profile": {
                  |      "display_name": "${displayName}"
                  |    }
                  |  }
                  |}
                  |""".stripMargin
      )
    } yield r


  def success2(mxid: String, displayName: String): Task[Response[Task]] =
    log.info(s"authentication successful for $mxid") *>
      Ok(s"""|{
            |  "auth": {
            |    "success": true,
            |    "mxid": "${mxid},
            |    "profile": {
            |      "display_name": "${displayName}"
            |    }
            |  }
            |}
            |""".stripMargin)


  def authenticate(authRequest: AuthRequest): Task[Either[String, UserInfo]] = {

    val credentialsForDomain = for {
      _ <- authRequest.user.checkMxDomain(kConfig.matrixDomain)
      user <- authRequest.user.usernameOnly
      password = authRequest.user.password
    } yield (user, password)

    val task: Task[UserInfo] = for {
      // checkMxDomain
      (u, pwd) <- Task.fromEither(credentialsForDomain)

      // fetch token
      authResp <- kClient.token(Username(u), pwd)

      token <- Task.fromEither(authResp match {
        case v: ValidAuthResponse => Right(v.access_token)
        case e: ErrorAuthResponse => Left(new Throwable(e.asJson.noSpaces))// TODO: better error tracing
      })
      userinfo <- kClient.userinfo(Username(u), token)
    } yield userinfo

    task.foldM(
      e => Task.fromEither(Right(Left(e.getMessage))),
      v => Task.fromEither(Right(Right(v)))
    )
  }


  def decodeAuthRequest(req: Request[Task])(f: AuthRequest => Task[Response[Task]]): Task[Response[Task]] = {
    req.decode[String] { body =>
      decode[AuthRequest](body) match {
        case Left(err) => BadRequest(err.getMessage)
        case Right(authReq) => f(authReq)
      }
    }
  }

  def unauthenticated(reason: String): Task[Either[String, UserInfo]] =
    Task.fromEither(Left(new Throwable(reason)))

  val matrixInternal: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case req @ POST -> Root / "identity" / "v1" / "check_credentials" =>
      decodeAuthRequest(req) { authReq =>
        for {
          either <- authenticate(authReq)
          resp <- either match {
            case Left(err) => failure(err)
            case Right(userinfo) => success(authReq.user.id, userinfo.name)
          }
        } yield resp

      }
  }



}
