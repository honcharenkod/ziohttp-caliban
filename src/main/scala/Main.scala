import caliban.ZHttpAdapter
import graphql.{ProfileApi, ProfileService}
import zhttp.http._
import zhttp.service.Server
import zio.ZIOAppDefault

object Main extends ZIOAppDefault {
  override def run =
    (for {
      interpreter <- ProfileApi.api.interpreter
      _ <- Server
        .start(
          8088,
          Http.collectHttp[Request] {
            case _ -> !! / "api" / "graphql" => ZHttpAdapter.makeHttpService(interpreter)
          }
        )
        .forever
    } yield ())
      .provide(
          ProfileService.make
      ).exitCode
}
