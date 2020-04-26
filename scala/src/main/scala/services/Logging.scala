package services

import zio._

object Logging {

  trait Service {
    def info(s: String): UIO[Unit]
    def error(s: String): UIO[Unit]
  }


  import zio.console.Console
  val consoleLogger: ZLayer[Console, Nothing, Logging] = ZLayer.fromFunction( console =>
    new Service {
      def info(s: String): UIO[Unit]  = console.get.putStrLn(s"info - $s")
      def error(s: String): UIO[Unit] = console.get.putStrLn(s"error - $s")
    }
  )

  //accessor methods
  def info(s: String): ZIO[Logging, Nothing, Unit] =
    ZIO.accessM(_.get.info(s))

  def error(s: String): ZIO[Logging, Nothing, Unit] =
    ZIO.accessM(_.get.error(s))
}
