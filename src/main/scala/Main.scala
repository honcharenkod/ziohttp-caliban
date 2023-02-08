import caliban.ZHttpAdapter
import dao.models.User
import dao.repositories.ProfileRepositoryImpl
import dao.{AuthInfoDAOImpl, UserDaoImpl}
import graphql.auth.Auth
import graphql._
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
      interpreter <- ProfileApi.api.interpreter
      _ <- Server
        .start(
          9000,
          Http.collectHttp[Request] {
            //case Method.GET -> !! / "text" => Response.text("Hello World!")
            case _ -> !! / "api" / "graphql" =>
              ZHttpAdapter.makeHttpService(interpreter) @@ Auth.middleware
          }
        ).forever
    } yield ())
      .provide(
        ZLayer.scoped(FiberRef.make(Option.empty[User])),
        ConfigService.live,
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("database"),
        UserDaoImpl.live,
        AuthInfoDAOImpl.live,
        ProfileRepositoryImpl.live,
        JWTService.live,
        PasswordService.live,
        Auth.live,
        ProfileService.live
      )
}
