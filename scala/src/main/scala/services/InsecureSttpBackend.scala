package services

import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl._
import org.asynchttpclient._
import sttp.client.SttpBackend
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._

object InsecureSttpBackend {

  type Service = SttpBackend[Task, Nothing, WebSocketHandler]

  private[this] val sslContext: SslContext = SslContextBuilder
    .forClient()
    .sslProvider(SslProvider.JDK)
    .trustManager(InsecureTrustManagerFactory.INSTANCE)
    .build()

  private[this]val config: DefaultAsyncHttpClientConfig.Builder = new DefaultAsyncHttpClientConfig.Builder()
    .setSslContext(sslContext)

  private[this]val insecureAsyncHttpClient: AsyncHttpClient = org.asynchttpclient.Dsl.asyncHttpClient(config.build())

  val insecureSttpBackend: SttpBackend[Task, Nothing, WebSocketHandler] = AsyncHttpClientZioBackend.usingClient(insecureAsyncHttpClient)

  val managed: TaskManaged[SttpBackend[Task, Nothing, WebSocketHandler]] = AsyncHttpClientZioBackend.managedUsingConfig(config.build())


}
