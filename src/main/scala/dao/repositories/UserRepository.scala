package dao.repositories

import dao.models.User

object UserRepository {

  type UserRepository = UserRepository.Service
  trait Service extends CommonCRUD [User]

}
