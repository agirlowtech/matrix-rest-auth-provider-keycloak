import config.KeycloakConfig
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze._
import services.{InsecureSttpBackend, KeycloakOIDClient, Logging, RestAuthProvider}
import zio._
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._

object MyApp extends App {

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {

    val program = for {
      httpClient <- makeInsecureSttpClientManaged
      _ <- makeProgram(httpClient)
    } yield ()

    program.foldM(
      e => putStrLn(s"Execution failed with: ${e.printStackTrace()}") *> ZIO.succeed(1),
      _ => ZIO.succeed(0)
    )
  }

  private def makeInsecureSttpClientManaged: UIO[TaskManaged[InsecureSttpBackend.Service]] =
    ZIO.runtime[Any].map { implicit rts =>
      InsecureSttpBackend.managed
    }

  val startServer: ZIO[KeycloakOIDClient with Has[KeycloakConfig] with Logging, Throwable, Unit] =
    ZIO.runtime[KeycloakOIDClient with Has[KeycloakConfig] with Logging]
      .flatMap {
        implicit rts =>

          val kClient = rts.environment.get[KeycloakOIDClient.Service]
          val kConfig = rts.environment.get[KeycloakConfig]
          val log = rts.environment.get[Logging.Service]

          val rap = RestAuthProvider(kClient, kConfig, log)

          val httpApp: HttpApp[Task] =  Router(
              "/ping" -> rap.pingService,
              "/_matrix-internal" -> rap.matrixInternal
            ).orNotFound

          for {
            _ <- log.info(s"Starting Server on 0.0.0.0:${kConfig.serverPort}...")
            _ <- BlazeServerBuilder[Task]
                    .bindHttp(kConfig.serverPort, "0.0.0.0")
                    .withHttpApp(httpApp)
                    .serve
                    .compile
                    .drain
          } yield ()
      }

  private def makeProgram(insHttpClient: TaskManaged[InsecureSttpBackend.Service])
  : RIO[ZEnv, Unit] = {

    val loggerLayer = Console.live >>> Logging.consoleLogger

    val insHttpClientLayer = insHttpClient.toLayer.orDie
    val kConfigLayer = KeycloakConfig.fromSystem().toLayer.orDie

    val kEnvLayer = (insHttpClientLayer ++ kConfigLayer ++ loggerLayer)

    val kClientLayer = kEnvLayer >>> KeycloakOIDClient.live
    val rServerLayer = kClientLayer ++ kConfigLayer ++ loggerLayer

    startServer.provideSomeLayer[ZEnv](rServerLayer)
  }

}
