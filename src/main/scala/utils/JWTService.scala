package utils

import dao.models.User
import dao.repositories.UserRepository.UserRepository
import graphql.ProfileService.ProfileService
import pdi.jwt._
import utils.config.ConfigService.{CommonConfig, ConfigService}
import zio._
import zio.json._

object JWTService {
  trait JWTService {
    def generateToken(user: User): Task[String]

    def validateToken(token: String): Task[User]
  }

  case class JWTServiceImpl(configService: ConfigService) extends JWTService {
    override def generateToken(user: User): Task[String] =
      ZIO.succeed {
        Jwt.encode(user.toJsonPretty, "secretKey", JwtAlgorithm.HS256)
      }

    override def validateToken(token: String): Task[User] =
      ZIO.fromTry {
        Jwt.decode(token, "secretKey", Seq(JwtAlgorithm.HS256))
          .map(_.content.fromJson[User])
      }
        .map(_.left.map(new Throwable(_)))
        .flatMap(ZIO.fromEither(_))
  }

  val live: ZLayer[ConfigService, Nothing, JWTService] = ZLayer {
    for {
      configService <- ZIO.service[ConfigService]
    } yield JWTServiceImpl(configService)
  }
}
