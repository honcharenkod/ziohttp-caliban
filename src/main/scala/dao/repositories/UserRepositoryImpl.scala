package dao.repositories

import dao.models.User
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio._

class UserRepositoryImpl(ctx: Quill.Postgres[SnakeCase]) extends UserRepository.Service {
  import ctx._

  private val users = quote {
    querySchema[User]("users", _.id -> "id", _.email -> "email", _.name -> "name")
  }

  override def create(entity: User): Task[User] =
    run(
      users.insertValue(entity)
    )

  override def read(id: Long): Task[Option[User]] =
    run(
      users.filter(_.id == id)
    ).map(_.headOption)

  override def update(entity: User): Task[Option[User]] =
    run(
      users.updateValue(entity)
    )

  override def delete(id: Long): Task[Unit] =
    run(
      users.filter(_.id == id).delete
    )
}

object UserRepositoryImpl {
  val live = ZLayer.fromFunction(new UserRepositoryImpl(_))
}