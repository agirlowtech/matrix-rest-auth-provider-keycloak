package services

import config.KeycloakConfig
import data._

import io.circe.parser.decode
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client._
import zio._

object KeycloakOIDClient {

  sealed trait AuthResponse

  case class ValidAuthResponse(
                                access_token: String,
                                expires_in: Int,
                                refresh_expires_in: Int,
                                refresh_token: String,
                                token_type: String,
                                id_token: String,
                                `not-before-policy`: Int,
                                session_state: String,
                                scope: String
                              ) extends AuthResponse

  case class ErrorAuthResponse(error: String, error_description: String) extends AuthResponse

  class KeycloakOIDError(msg: String) extends Exception {
    override def getMessage: String = msg
  }
  case class UnexpectedError(msg: String) extends KeycloakOIDError(msg)
  case class DecodingError(msg: String) extends KeycloakOIDError(msg)
  case class BadResponseError(msg: String) extends KeycloakOIDError(msg)
  case class InvalidCredentials(msg: String) extends KeycloakOIDError(msg)

  type SttpBackendA = SttpBackend[Task, Nothing, WebSocketHandler]

  type R = Has[SttpBackendA] with Has[KeycloakConfig] with Logging

  trait Service {
    def token(username: Username, password: String): IO[KeycloakOIDError, AuthResponse]
    def userinfo(username: Username, access_token: String): IO[KeycloakOIDError, UserInfo]
  }

  // accessor methods
  def token(username: Username, password: String): ZIO[KeycloakOIDClient, KeycloakOIDError, AuthResponse] =
    ZIO.accessM(_.get.token(username, password))

  def userinfo(username: Username, access_token: String): ZIO[KeycloakOIDClient, KeycloakOIDError, UserInfo] =
    ZIO.accessM(_.get.userinfo(username, access_token))

  def authUser(username: Username, password: String): ZIO[KeycloakOIDClient, KeycloakOIDError, Either[String, UserInfo]] =
    for {
      resp <- token(username, password)
      userinfo <- resp match {
        case v: ValidAuthResponse =>
          userinfo(username, v.access_token).map(Right(_))
        case _ =>
          ZIO.fromFunctionM[KeycloakOIDClient, KeycloakOIDError, Either[String, UserInfo]] {
            _ =>
              ZIO.fromEither(
                Right(
                  Left(s"unknown username or bad credentials for '$username'")
                )
              )
          }
      }

    } yield userinfo

  // ===============
  // implementations

  val live: ZLayer[R, Nothing, KeycloakOIDClient] =
    ZLayer.fromServices[SttpBackendA, KeycloakConfig, Logging.Service, Service] { (sttpClient, config, log) =>
      new Service {

        def jsonDecodeResponse[A: Decoder](resp: Response[Either[String, String]]): Either[KeycloakOIDError, A] =
        {
          for {
            s <- resp.body.left.map(UnexpectedError)
            d <- decode[A](s).left.map(e => DecodingError(e.getMessage))
          } yield d
        }

        def badResponseError[A](resp: Response[Either[String, String]]): Either[KeycloakOIDError, A] =
          Left(BadResponseError(s"unexpected status code ${resp.code.code} for resp with body ${resp.body}"))

        def decodeTokenResponse(resp: Response[Either[String, String]]): Either[KeycloakOIDError, AuthResponse] = {
          (resp.code.code match {
            case 200 => jsonDecodeResponse[ValidAuthResponse](resp)
            case 401 => jsonDecodeResponse[ErrorAuthResponse](resp)
            case _ => badResponseError[AuthResponse](resp)
          })
        }

        def decodeUserinfoResponse(resp: Response[Either[String, String]]): Either[KeycloakOIDError, UserInfo] = {
          resp.code.code match {
            case 200 => jsonDecodeResponse[UserInfo](resp)
            case _ => badResponseError[UserInfo](resp)
          }
        }

        override def token(username: Username, password: String): IO[KeycloakOIDError, AuthResponse] =
          for {
            _ <- log.info(s"requesting access_token for $username")

            params = Map(
              "client_id" -> config.clientId,
              "grant_type" -> "password",
              "client_secret" -> config.clientSecret,
              "scope" -> "openid",
              "username" -> username.value,
              "password" -> password
            )

            req = basicRequest.body(params)
              .post(config.openIdUri)
              .response(asString)

            hiddenParams = params.updated("password", "**********").updated("client_secret", "**********")

            _ <- log.info(s"sending request with params ${hiddenParams} | body = ${req.body}")

            resp <- sttpClient.send(req).mapError(e => BadResponseError(e.getMessage))
            dec <- IO fromEither decodeTokenResponse(resp)

            _ <- log.info(s"retrieved access_token for $username")
          } yield dec

        override def userinfo(username: Username, access_token: String): IO[KeycloakOIDError, UserInfo] =
          for {
            _ <- log.info(s"getting userinfo for ${username.asJson.noSpaces}")

            req = basicRequest
              .get(config.userInfoUri)
              .header("Authorization", s"Bearer $access_token")
              .response(asString)

            resp <- sttpClient.send(req).mapError(e => BadResponseError(e.getMessage))

            userinfo <- IO fromEither decodeUserinfoResponse(resp)

            _ <- log.info(s"$username userinfo is ${userinfo.asJson.noSpaces}")
          } yield userinfo
      }
  }
}
