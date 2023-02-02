package graphql

import dao.models.User
import dao.repositories.UserRepository.UserRepository
import zio._

object ProfileService {
  trait ProfileService {
    def getUserInfo(userId: Long): RIO[ProfileService, Option[User]]
  }
  case class ProfileServiceImpl(userRepository: UserRepository) extends ProfileService {
    override def getUserInfo(userId: Long): RIO[ProfileService, Option[User]] =
      userRepository.read(userId)
  }

  def getUserInfo(userId: Long): RIO[ProfileService, Option[User]] =
    ZIO.serviceWithZIO(_.getUserInfo(userId))

  val live: ZLayer[UserRepository, Nothing, ProfileService] = ZLayer {
    for {
      userRepository <- ZIO.service[UserRepository]
    } yield ProfileServiceImpl(userRepository)
  }
}
