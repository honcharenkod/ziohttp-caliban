package graphql

import dao.models._
import dao.repositories.MessageRepository.MessageRepository
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
  trait GraphqlService {
    def signUp(email: String, name: String, surname: String, password: String): RIO[GraphqlService, User]

    def signIn(email: String, password: String): RIO[GraphqlService, String]

    def subscribeNotifications: ZStream[Auth, Throwable, Notification]

    def sendMessage(text: String, recipientId: Long): RIO[GraphqlService with Auth, Message]
  }

  private case class ProfileServiceImpl(profileRepository: ProfileRepository,
                                        jwtService: JWTService,
                                        passwordService: PasswordService,
                                        messageRepository: MessageRepository,
                                        subscribers: Hub[Notification]) extends GraphqlService {

    override def signUp(email: String, name: String, surname: String, password: String): RIO[GraphqlService, User] =
      for {
        hashedPassword <- passwordService.hashPassword(password)
        user <-
          profileRepository.signUp(email, name, surname, hashedPassword)
      } yield user

    override def signIn(email: String, password: String): RIO[GraphqlService, String] =
      for {
        userInfo <- profileRepository.getUserWithAuthInfoByEmail(email)
          .flatMap(ZIO.fromOption(_).mapError(_ => InvalidCredentialsException))
        _ <-
          passwordService.validatePassword(password, userInfo._2.hashedPassword)
        token <- jwtService.generateToken(userInfo._1)
      } yield token

    override def subscribeNotifications: ZStream[Auth, Throwable, Notification] =
      ZStream.scoped(
        for {
          userId <- Auth.user.map(_.id)
          notifications <- subscribers.subscribe.flatMap(_.takeAll.map(_.filter(_.recipientId == userId)))
        } yield notifications
      ).flatMap(ZStream.fromChunk(_))

    override def sendMessage(text: String, recipientId: Long): RIO[GraphqlService with Auth, Message] =
      for {
        userId <- Auth.user.map(_.id)
        message <- messageRepository.sendMessage(text, userId, recipientId)
      } yield message
  }

  def signUp(email: String, name: String, surname: String, password: String): RIO[GraphqlService, User] =
    ZIO.serviceWithZIO(_.signUp(email, name, surname, password))

  def signIn(email: String, password: String): RIO[GraphqlService, String] =
    ZIO.serviceWithZIO(_.signIn(email, password))

  def subscribeNotifications: ZStream[Auth with GraphqlService, Throwable, Notification] =
    ZStream.serviceWithStream[GraphqlService](_.subscribeNotifications)

  def sendMessage(text: String, recipientId: Long): RIO[GraphqlService with Auth, Message] =
    ZIO.serviceWithZIO[GraphqlService](_.sendMessage(text, recipientId))

  val live: ZLayer[ProfileRepository with JWTService with MessageRepository with PasswordService, Nothing, GraphqlService] = ZLayer {
    for {
      subscribers <- Hub.unbounded[Notification]
      userRepository <- ZIO.service[ProfileRepository]
      jwtService <- ZIO.service[JWTService]
      messageRepository <- ZIO.service[MessageRepository]
      passwordService <- ZIO.service[PasswordService]
    } yield ProfileServiceImpl(userRepository, jwtService, passwordService, messageRepository, subscribers)
  }
}
