package graphql

import dao.models._
import dao.repositories.MessageRepository.MessageRepository
import dao.repositories.ProfileRepository.ProfileRepository
import exceptions.InvalidCredentials
import graphql.auth.Auth
import graphql.auth.Auth.{Auth, user}
import models.Notification
import utils.auth.JWTService.JWTService
import utils.auth.PasswordService.PasswordService
import zio._
import zio.stream.ZStream
import zio.concurrent._

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
                                        subscribersRef: Ref[ConcurrentMap[Long, Hub[Notification]]]) extends GraphqlService {

    override def signUp(email: String, name: String, surname: String, password: String): RIO[GraphqlService, User] =
      for {
        hashedPassword <- passwordService.hashPassword(password)
        user <-
          profileRepository.signUp(email, name, surname, hashedPassword)
      } yield user

    override def signIn(email: String, password: String): RIO[GraphqlService, String] =
      for {
        userInfo <- profileRepository.getUserWithAuthInfoByEmail(email)
          .flatMap(ZIO.fromOption(_).mapError(_ => InvalidCredentials))
        _ <-
          passwordService.validatePassword(password, userInfo._2.hashedPassword)
        token <- jwtService.generateToken(userInfo._1)
      } yield token


    //TODO: Fix notifications
    override def subscribeNotifications: ZStream[Auth, Throwable, Notification] = {
      ZStream.fromZIO(
        for {
          userId <- Auth.user.map(_.id)
          _ <- test(userId)
          hub <- subscribersRef.get.flatMap(_.get(userId)).flatMap(ZIO.fromOption(_).mapError(_ => new Throwable()))
        } yield hub
      ).flatMap(ZStream.fromHub(_))
    }

    private def test(userId: Long): Task[Unit] = {
      for {
        hub <- Hub.unbounded[Notification]
        subscribers <- subscribersRef.get
        _ <- subscribers.put(userId, hub)
        _ <- subscribersRef.update(_ => subscribers)
      } yield {}
    }

    override def sendMessage(text: String, recipientId: Long): RIO[GraphqlService with Auth, Message] =
      for {
        userId <- Auth.user.map(_.id)
        message <- messageRepository.sendMessage(text, userId, recipientId)
        _ <- subscribersRef.get
          .flatMap(_.get(recipientId)
            .map(
              _.map(
                _.publish(
                  Notification(
                    userId,
                    recipientId,
                    text
                  )
                )
              )
            )
          )
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
      map <- ConcurrentMap.empty[Long, Hub[Notification]]
      subscribers <- Ref.make(map)
      userRepository <- ZIO.service[ProfileRepository]
      jwtService <- ZIO.service[JWTService]
      messageRepository <- ZIO.service[MessageRepository]
      passwordService <- ZIO.service[PasswordService]
    } yield ProfileServiceImpl(userRepository, jwtService, passwordService, messageRepository, subscribers)
  }
}
