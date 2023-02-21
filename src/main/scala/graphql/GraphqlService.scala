package graphql

import dao.models.{AuthInfo, User}
import dao.repositories.ProfileRepository.ProfileRepository
import exceptions.InvalidCredentialsException
import graphql.auth.Auth
import graphql.auth.Auth.Auth
import models.Notification
import utils.auth.JWTService.JWTService
import utils.auth.PasswordService.PasswordService
import zio._
import zio.stream.ZStream

object GraphqlService {
  trait ProfileService {
    def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User]

    def signIn(email: String, password: String): RIO[ProfileService, String]

    def subscribeNotifications: ZStream[Auth, Throwable, Notification]
  }

  private case class ProfileServiceImpl(profileRepository: ProfileRepository,
                                        jwtService: JWTService,
                                        passwordService: PasswordService,
                                        subscribers: Hub[Notification]) extends ProfileService {

    override def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User] =
      for {
        hashedPassword <- passwordService.hashPassword(password)
        user <-
          profileRepository.signUp(email, name, surname, hashedPassword)
      } yield user

    override def signIn(email: String, password: String): RIO[ProfileService, String] =
      for {
        userInfo <- profileRepository.getUserWithAuthInfoByEmail(email)
          .flatMap(ZIO.fromOption(_).mapError(_ => InvalidCredentialsException))
        _ <-
          passwordService.validatePassword(password, userInfo._2.hashedPassword)
        token <- jwtService.generateToken(userInfo._1)
      } yield token

    override def subscribeNotifications: ZStream[Auth, Throwable, Notification] = {
        ZStream.fromChunk(Chunk(Notification(1, 2, "test1"), Notification(1, 2, "test2")))
    }
  }

  def signUp(email: String, name: String, surname: String, password: String): RIO[ProfileService, User] =
    ZIO.serviceWithZIO(_.signUp(email, name, surname, password))

  def signIn(email: String, password: String): RIO[ProfileService, String] =
    ZIO.serviceWithZIO(_.signIn(email, password))

  def subscribeNotifications: ZStream[Auth with ProfileService, Throwable, Notification] =
    ZStream.serviceWithStream[ProfileService](_.subscribeNotifications)

  val live: ZLayer[ProfileRepository with JWTService with PasswordService, Nothing, ProfileService] = ZLayer {
    for {
      subscribers <- Hub.unbounded[Notification]
      userRepository <- ZIO.service[ProfileRepository]
      jwtService <- ZIO.service[JWTService]
      passwordService <- ZIO.service[PasswordService]
    } yield ProfileServiceImpl(userRepository, jwtService, passwordService, subscribers)
  }
}
