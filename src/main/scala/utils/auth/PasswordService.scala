package utils.auth

import io.github.nremond.SecureHash
import utils.config.ConfigService.ConfigService
import zio.{Task, ZIO, ZLayer}

object PasswordService {
  trait PasswordService {
    def hashPassword(password: String): Task[String]
    def validatePassword(password: String, hashedPassword: String): Task[Boolean]
  }

  case class PasswordHashingServiceImpl(config: ConfigService) extends PasswordService {
    override def hashPassword(password: String): Task[String] =
      ZIO.succeed {
        SecureHash.createHash(password)
      }

    override def validatePassword(password: String, hashedPassword: String): Task[Boolean] =
      ZIO.succeed {
        SecureHash.validatePassword(password, hashedPassword)
      }
  }

  val live: ZLayer[ConfigService, Nothing, PasswordService] = ZLayer {
    for {
      config <- ZIO.service[ConfigService]
    } yield PasswordHashingServiceImpl(config)
  }
}
