import caliban.ZHttpAdapter
import dao.repositories.UserRepositoryImpl
import graphql.{ProfileApi, ProfileService}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zhttp.http._
import zhttp.service.Server
import zio.ZIOAppDefault

object Main extends ZIOAppDefault {
  override def run =
    (for {
      interpreter <- ProfileApi.api.interpreter
      _ <- Server
        .start(
          9000,
          Http.collectHttp[Request] {
            //case Method.GET -> !! / "text" => Response.text("Hello World!")
            case _ -> !! / "api" / "graphql" => ZHttpAdapter.makeHttpService(interpreter)
          }
        ).forever
    } yield ())
      .provide(
        Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("database"),
        UserRepositoryImpl.live,
        ProfileService.live
      ).exitCode
}
