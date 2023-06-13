package utils.auth.jwt

import dao.models.User
import pdi.jwt._
import utils.config.ConfigService.Config
import zio.json._
import zio.{Task, ZIO, ZLayer}

class JWTServiceImpl(config: Config) extends JWTService {
  private val jwtConfig = config.get.jwt

  override def generateToken(user: User): Task[String] =
    ZIO.succeed {
      Jwt.encode(user.toJsonPretty, jwtConfig.secretKey, JwtAlgorithm.HS256)
    }

  override def validateToken(token: String): Task[User] =
    ZIO.fromTry {
      Jwt.decode(token, jwtConfig.secretKey, Seq(JwtAlgorithm.HS256))
        .map(_.content.fromJson[User])
    }
      .map(_.left.map(new Throwable(_)))
      .flatMap(ZIO.fromEither(_))

}

object JWTServiceImpl {
  val live: ZLayer[Config, Nothing, JWTService] = ZLayer {
    for {
      config <- ZIO.service[Config]
    } yield new JWTServiceImpl(config)
  }
}
