package dao.repositories

import dao.models._
import zio.Task

object ProfileRepository {

  type ProfileRepository = ProfileRepository.Service
  trait Service {
    def signUp(email: String, name: String, surname: String, password: String): Task[User]
  }

}
