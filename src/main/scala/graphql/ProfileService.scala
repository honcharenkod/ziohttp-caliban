package graphql

import dao.models.User
import dao.repositories.UserRepository.UserRepository
import zio._

object ProfileService {
  trait ProfileService {
    def getUserInfo(userId: Long): RIO[ProfileService, Option[User]]
    def signUp(email: String, name: String): RIO[ProfileService, User]
  }
  case class ProfileServiceImpl(userRepository: UserRepository) extends ProfileService {
    override def getUserInfo(userId: Long): RIO[ProfileService, Option[User]] =
      userRepository.read(userId)

    override def signUp(email: String, name: String): RIO[ProfileService, User] =
      userRepository.create(
        User(
          0,
          email,
          name
        )
      )
  }

  def getUserInfo(userId: Long): RIO[ProfileService, Option[User]] =
    ZIO.serviceWithZIO(_.getUserInfo(userId))

  def signUp(email: String, name: String): RIO[ProfileService, User] =
    ZIO.serviceWithZIO(_.signUp(email: String, name: String))

  val live: ZLayer[UserRepository, Nothing, ProfileService] = ZLayer {
    for {
      userRepository <- ZIO.service[UserRepository]
    } yield ProfileServiceImpl(userRepository)
  }
}
