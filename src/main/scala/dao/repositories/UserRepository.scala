package dao.repositories

import dao.models.User
import io.getquill.{PostgresZioJdbcContext, SnakeCase}
import io.getquill.jdbczio.Quill
import io.getquill.mirrorContextWithQueryProbing._
import zio.ZLayer

class UserRepository(ctx: Quill.Postgres[SnakeCase]) {

  private val users = quote {
    querySchema[User]("users", _.id -> "id", _.email -> "email", _.name -> "name")
  }

  import ctx._
  def read(id: Long) =
    run(users.filter(_.id == lift(id))).map(_.headOption)
}

object UserRepository {
  val live = ZLayer.fromFunction(new UserRepository(_))
}