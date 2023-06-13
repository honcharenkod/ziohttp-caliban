package utils.auth.jwt

import dao.models.User
import zio.Task

trait JWTService {
  def generateToken(user: User): Task[String]

  def validateToken(token: String): Task[User]
}
