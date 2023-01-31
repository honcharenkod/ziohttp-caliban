package graphql

import dao.models.User
import zio._

object ProfileService {
  trait ProfileService {
    def getUserInfo(userId: Long): RIO[ProfileService, Option[User]]
  }

  def getUserInfo(userId: Long): RIO[ProfileService, Option[User]] =
    ZIO.serviceWithZIO(_.getUserInfo(userId))

  def make = ZLayer.fromZIO {
    ZIO.succeed {
      new ProfileService {
        override def getUserInfo(userId: Long): RIO[ProfileService, Option[User]] = ???
      }
    }
  }
}
