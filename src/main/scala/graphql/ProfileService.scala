package graphql

import dao.models.{AuthInfo, User}
import dao.repositories.ProfileRepository.ProfileRepository
import exceptions.InvalidCredentialsException
import utils.auth.JWTService.JWTService
import utils.auth.PasswordService.PasswordService
import zio._

object ProfileService {
  trait ProfileService {
    def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User]
    def signIn(email: String, password: String): RIO[ProfileService, String]
  }

  case class ProfileServiceImpl(profileRepository: ProfileRepository,
                                jwtService: JWTService,
                                passwordService: PasswordService) extends ProfileService {

    override def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User] =
      for {
        hashedPassword <- passwordService.hashPassword(password)
        user <-
          profileRepository.signUp(email, name, surname, hashedPassword)
      } yield user

    override def signIn(email: String, password: String): RIO[ProfileService, String] =
      for {
        userInfo <- profileRepository.getUserWithAuthInfoByEmail(email)
          .flatMap(ZIO.fromOption(_).mapError(_ => new InvalidCredentialsException))
        _ <-
          passwordService.validatePassword(password, userInfo._2.hashedPassword)
        token <- jwtService.generateToken(userInfo._1)
      } yield token
  }
  def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User] =
    ZIO.serviceWithZIO(_.signUp(email, name, surname, password))
  def signIn(email: String, password: String): RIO[ProfileService, String] =
    ZIO.serviceWithZIO(_.signIn(email, password))

  val live: ZLayer[ProfileRepository with JWTService with PasswordService, Nothing, ProfileService] = ZLayer {
    for {
      userRepository <- ZIO.service[ProfileRepository]
      jwtService <- ZIO.service[JWTService]
      passwordService <- ZIO.service[PasswordService]
    } yield ProfileServiceImpl(userRepository, jwtService, passwordService)
  }
}
