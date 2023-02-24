package graphql.auth

import caliban.Value.StringValue
import caliban.interop.tapir.{StreamTransformer, WebSocketHooks}
import caliban._
import dao.models._
import exceptions.Unauthorized
import utils.auth.JWTService.JWTService
import zhttp.http._
import zio._
import zio.stream.ZStream

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
      user <- ZIO.serviceWithZIO[Authentication](_.get.flatMap(ZIO.fromOption(_).mapError(_ => Unauthorized)))
      result <- ZIO.serviceWithZIO[AuthService](_.hasRole(roles, user)) *> action
    } yield result

  val user = ZIO.serviceWithZIO[Authentication](_.get.flatMap(ZIO.fromOption(_).mapError(_ => Unauthorized)))

  val live =
    ZLayer.fromFunction { () =>
      new AuthService {
        override def hasRole(roles: Seq[Role], user: User): Task[Unit] =
          ZIO.fromOption(
            roles.find(role => user.role == role)
          )
            .mapError(_ => Unauthorized)
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

  val WSHooks = WebSocketHooks.init[Authentication with JWTService, CalibanError] { payload =>
    (payload match {
      case InputValue.ObjectValue(fields) =>
        fields.get("Authorization") match {
          case Some(token: StringValue) =>
            ZIO.serviceWithZIO[JWTService](_.validateToken(token.value)).map(Some(_))
              .mapError(e => CalibanError.ExecutionError(e.getMessage))
          case _ => ZIO.fail()
        }
      case _ => ZIO.fail()
    })
      .orElseFail(CalibanError.ExecutionError("Unable to decode payload"))
      .flatMap(user => ZIO.serviceWithZIO[Authentication](_.set(user)))
  } ++
    WebSocketHooks.afterInit(ZIO.failCause(Cause.empty).delay(10.seconds))

}