package utils.auth

import exceptions.InvalidCredentials
import io.github.nremond.SecureHash
import utils.config.ConfigService.ConfigService
import zio.{Task, ZIO, ZLayer}

object PasswordService {
  trait PasswordService {
    def hashPassword(password: String): Task[String]

    def validatePassword(password: String, hashedPassword: String): Task[Unit]
  }

  case class PasswordHashingServiceImpl(config: ConfigService) extends PasswordService {
    override def hashPassword(password: String): Task[String] =
      ZIO.succeed {
        SecureHash.createHash(password)
      }

    override def validatePassword(password: String, hashedPassword: String): Task[Unit] =
      if (SecureHash.validatePassword(password, hashedPassword))
        ZIO.unit
      else ZIO.fail(InvalidCredentials)
  }

  val live: ZLayer[ConfigService, Nothing, PasswordService] = ZLayer {
    for {
      config <- ZIO.service[ConfigService]
    } yield PasswordHashingServiceImpl(config)
  }
}
