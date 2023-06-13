package utils.auth

import exceptions.InvalidCredentials
import io.github.nremond.SecureHash
import utils.auth
import zio.{Task, ZIO, ZLayer}

object PasswordService {

  type PasswordService = Service

  trait Service {
    def hashPassword(password: String): Task[String]

    def validatePassword(password: String, hashedPassword: String): Task[Unit]
  }

  val live: ZLayer[Any, Nothing, PasswordService] = ZLayer.succeed {
    new Service {
      override def hashPassword(password: String): Task[String] =
        ZIO.succeed {
          SecureHash.createHash(password)
        }

      override def validatePassword(password: String, hashedPassword: String): Task[Unit] =
        if (SecureHash.validatePassword(password, hashedPassword))
          ZIO.unit
        else ZIO.fail(InvalidCredentials)
    }
  }
}
