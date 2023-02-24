import caliban.ZHttpAdapter
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
import zhttp.http._
import zhttp.service.Server
import zio._

object Main extends ZIOAppDefault {
  override def run =
    (for {
      interpreter <- GraphqlApi.api.interpreter
      _ <- Server
        .start(
          9000,
          Http.collectHttp[Request] {
            //case Method.GET -> !! / "text" => Response.text("Hello World!")
            case _ -> !! / "api" / "graphql" =>
              ZHttpAdapter.makeHttpService(interpreter) @@ Auth.middleware
            case _ -> !! / "api" / "subscriptions" =>
              ZHttpAdapter.makeWebSocketService(interpreter, webSocketHooks = Auth.WSHooks)
          }
        ).forever
    } yield ())
      .provide(
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
        GraphqlService.live
      )
}
