package object data {

  case class AuthRequest(user: User)

  case class User(id: String, password: String) {

    def usernameOnly: Either[Throwable, String] = id.split(":").toList match {
      case atusername :: (_ :: Nil) =>
        Right(atusername.replaceAll("@", ""))
      case _ =>
        Left(new Throwable(s"invalid matrix id : got '$id' but expecting format '@username:matrix.domain.org'"))
    }

    def checkMxDomain(expMxDomain: String): Either[Throwable, Unit] =
    {
      id.split(":").toList match {
        case _ :: Nil => Left(new Throwable(s"matrix domain is missing : expecting '@username:matrix.domain.org'"))
        case Nil => Left(new Throwable(s"empty matrix id : expecting '@username:matrix.domain.org'"))
        case _ :: (mxdomain :: Nil) =>
          if (mxdomain equals expMxDomain) Right(())
          else Left(new Throwable(s"unexpected matrix domain : got '$mxdomain' but '$expMxDomain' expected"))
        case _ =>
          Left(new Throwable(s"invalid matrix id : expecting '@username:matrix.domain.org'"))
      }
    }
  }

  case class Username(value: String)

  case class UserInfo(email: String, name: String, preferred_username: String)

  case class AuthResponse(auth: AuthObject)
  case class AuthObject(success: Boolean, mxid: String, profile: Profile)
  case class Profile(display_name: String, three_pids: List[ThreePID])
  case class ThreePID(medium: String, address: String)

  case class KeycloakResponseValid(access_token: String)
}
