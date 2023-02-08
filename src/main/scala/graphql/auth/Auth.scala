package graphql.auth

import dao.models._
import utils.auth.JWTService.JWTService
import zhttp.http.HttpError.Unauthorized
import zhttp.http._
import zio._

object Auth {
  type Authentication = FiberRef[Option[User]]
  type Auth = Authentication with AuthService

  trait AuthService {
    def hasRole(roles: Seq[Role], user: User): Task[Unit]
  }

  def authorize[R <: AuthService, A](roles: Role*)(implicit action: RIO[R, A]): RIO[R with Authentication, A] = {
    if (roles.isEmpty)
      authorize
    else
      commonAuthorize(roles)
  }

  private def authorize[R <: AuthService, A](implicit action: RIO[R, A]): RIO[R with Authentication, A] =
    commonAuthorize(Seq(Role.User, Role.Admin))

  private def commonAuthorize[R <: AuthService, A](roles: Seq[Role])(implicit action: RIO[R, A]): RIO[R with Authentication, A] =
    for {
      user <- ZIO.serviceWithZIO[Authentication](_.get.flatMap(ZIO.fromOption(_).mapError(_ => Unauthorized.asInstanceOf[Throwable])))
      result <- ZIO.serviceWithZIO[AuthService](_.hasRole(roles, user)) *> action
    } yield result

  val user = ZIO.serviceWithZIO[Authentication](_.get.flatMap(ZIO.fromOption(_).mapError(_ => Unauthorized.asInstanceOf[Throwable])))

  val live =
    ZLayer.fromFunction { () =>
      new AuthService {
        override def hasRole(roles: Seq[Role], user: User): Task[Unit] =
          ZIO.fromOption(
            roles.find(role => user.role == role)
          )
            .mapError(_ => Unauthorized.asInstanceOf[Throwable])
            .map(_ => {})
      }
    }

  def middleware = new Middleware[JWTService with Authentication, Throwable, Request, Response, Request, Response] {
    override def apply[R1 <: JWTService with Authentication, E1 >: Throwable](http: Http[R1, E1, Request, Response]): Http[R1, E1, Request, Response] =
      http.contramapZIO[R1, E1, Request] { request =>
        for {
          token <- ZIO.succeed(request.authorization.map(_.toString))
          user <- token match {
            case Some(token) =>
              ZIO.serviceWithZIO[JWTService](_.validateToken(token)).map(Some(_))
            case None => ZIO.succeed(None)
          }
          _ <- ZIO.serviceWithZIO[Authentication](_.set(user))
        } yield request
      }
  }
}