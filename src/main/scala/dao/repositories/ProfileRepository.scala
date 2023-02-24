package dao.repositories

import dao.models._
import zio._

object ProfileRepository {

  type ProfileRepository = ProfileRepository.Service
  trait Service {
    def signUp(email: String, name: String, surname: String, password: String): Task[User]
    def getUserWithAuthInfoByEmail(email: String): Task[Option[(User, AuthInfo)]]
    def uploadProfilePhoto(userId: Long, data: Chunk[Byte], mimeType: String): Task[ProfilePhoto]
  }

}
