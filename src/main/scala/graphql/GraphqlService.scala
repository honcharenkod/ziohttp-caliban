package graphql

import caliban.uploads.{Upload, Uploads}
import dao.models._
import dao.repositories.{MessageRepository, ProfileRepository}
import exceptions.InvalidCredentials
import graphql.auth.Auth
import graphql.auth.Auth._
import models.Notification
import utils.auth.PasswordService.PasswordService
import utils.auth.jwt.JWTService
import zio._
import zio.stream.ZStream

object GraphqlService {

  type GraphqlService = Service

  trait Service {
    def signUp(email: String, name: String, surname: String, password: String): RIO[GraphqlService, User]

    def signIn(email: String, password: String): RIO[GraphqlService, String]

    def subscribeNotifications: ZStream[Auth, Throwable, Notification]

    def sendMessage(text: String, recipientId: Long): RIO[GraphqlService with Auth, Message]

    def uploadProfilePhoto(photo: Upload): RIO[GraphqlService with Auth with Uploads, Unit]
  }

  def signUp(email: String, name: String, surname: String, password: String): RIO[GraphqlService, User] =
    ZIO.serviceWithZIO(_.signUp(email, name, surname, password))

  def signIn(email: String, password: String): RIO[GraphqlService, String] =
    ZIO.serviceWithZIO(_.signIn(email, password))

  def subscribeNotifications: ZStream[Auth with GraphqlService, Throwable, Notification] =
    ZStream.serviceWithStream[GraphqlService](_.subscribeNotifications)

  def sendMessage(text: String, recipientId: Long): RIO[GraphqlService with Auth, Message] =
    ZIO.serviceWithZIO[GraphqlService](_.sendMessage(text, recipientId))

  def uploadProfilePhoto(photo: Upload): RIO[GraphqlService with Auth with Uploads, Unit] =
    ZIO.serviceWithZIO[GraphqlService](_.uploadProfilePhoto(photo))

  val live: ZLayer[PasswordService with MessageRepository with JWTService with ProfileRepository, Nothing, GraphqlService] =
    ZLayer {
      for {
        subscribers <- Hub.unbounded[Notification]
        profileRepository <- ZIO.service[ProfileRepository]
        jwtService <- ZIO.service[JWTService]
        messageRepository <- ZIO.service[MessageRepository]
        passwordService <- ZIO.service[PasswordService]
      } yield
        new Service {
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


          override def subscribeNotifications: ZStream[Auth, Throwable, Notification] = {
            for {
              userId <- ZStream.fromZIO(Auth.user.map(_.id))
              result <-
                ZStream.fromHub(subscribers).filter(_.recipientId == userId)
            } yield result
          }

          override def sendMessage(text: String, recipientId: Long): RIO[GraphqlService with Auth, Message] =
            for {
              userId <- Auth.user.map(_.id)
              message <- messageRepository.sendMessage(text, userId, recipientId)
              _ <- subscribers.publish(
                Notification(
                  userId,
                  recipientId,
                  text
                )
              )
            } yield message

          override def uploadProfilePhoto(photo: Upload): RIO[GraphqlService with Auth with Uploads, Unit] =
            for {
              userId <- Auth.user.map(_.id)
              bytes <- photo.allBytes
              mime <- photo.meta.map(_.flatMap(_.contentType))
                .flatMap(ZIO.fromOption(_).mapError(_ => new Throwable("Invalid mime-type")))
              _ <- profileRepository.uploadProfilePhoto(userId, bytes, mime)
            } yield {}
        }
  }
}
