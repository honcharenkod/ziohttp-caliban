package graphql

import dao.models.{AuthInfo, User}
import dao.repositories.ProfileRepository.ProfileRepository
import utils.auth.JWTService.JWTService
import utils.auth.PasswordService.PasswordService
import zio._

object ProfileService {
  trait ProfileService {
    def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User]
  }

  case class ProfileServiceImpl(userRepository: ProfileRepository,
                                jwtService: JWTService,
                                passwordService: PasswordService) extends ProfileService {

    override def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User] =
      for {
        hashedPassword <- passwordService.hashPassword(password)
        user <-
          userRepository.signUp(email, name, surname, hashedPassword)
      } yield user
  }
  def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User] =
    ZIO.serviceWithZIO(_.signUp(email, name, surname, password))

  val live: ZLayer[ProfileRepository with JWTService with PasswordService, Nothing, ProfileService] = ZLayer {
    for {
      userRepository <- ZIO.service[ProfileRepository]
      jwtService <- ZIO.service[JWTService]
      passwordService <- ZIO.service[PasswordService]
    } yield ProfileServiceImpl(userRepository, jwtService, passwordService)
  }
}
