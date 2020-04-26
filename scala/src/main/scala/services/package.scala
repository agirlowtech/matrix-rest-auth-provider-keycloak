import zio.Has

package object services {
  type KeycloakOIDClient = Has[KeycloakOIDClient.Service]
  type Logging = Has[Logging.Service]
  type InsecureSttpClient = Has[InsecureSttpBackend.Service]
}
