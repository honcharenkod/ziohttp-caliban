import caliban.ZHttpAdapter
import caliban.interop.tapir.{HttpInterpreter, WebSocketInterpreter}
import caliban.uploads.Uploads
import dao.models.User
import dao.repositories.{MessageRepositoryImpl, ProfileRepositoryImpl}
import dao.{AuthInfoDAOImpl, MessageDAOImpl, ProfilePhotoDAOImpl, UserDaoImpl}
import graphql._
import graphql.auth.Auth
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import utils.auth.{JWTService, PasswordService}
import utils.config.ConfigService
import zio._
import zio.http._

object Main extends ZIOAppDefault {
  import sttp.tapir.json.circe._

  override def run =
    (for {
      interpreter <- GraphqlApi.api.interpreter
      _ <- Server
        .serve(
          Http.collectHttp[Request] {
            //case Method.GET -> !! / "text" => Response.text("Hello World!")
            case _ -> !! / "api" / "graphql" => ZHttpAdapter.makeHttpService(HttpInterpreter(interpreter)) @@ Auth.middleware
            case _ -> !! / "api" / "subscriptions" =>
              ZHttpAdapter.makeWebSocketService(WebSocketInterpreter(interpreter, webSocketHooks = Auth.WSHooks))
          }
        ).forever
    } yield ())
      .provide(
        Scope.default,
        Uploads.empty,
        ZLayer.scoped(FiberRef.make(Option.empty[User])),
        ConfigService.live,
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("database"),
        UserDaoImpl.live,
        AuthInfoDAOImpl.live,
        ProfilePhotoDAOImpl.live,
        ProfileRepositoryImpl.live,
        MessageDAOImpl.live,
        MessageRepositoryImpl.live,
        JWTService.live,
        PasswordService.live,
        Auth.live,
        GraphqlService.live,
        ZLayer.succeed(Server.Config.default.port(9000)),
        Server.live
      )
}
