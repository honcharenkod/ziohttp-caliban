package dao.repositories

import dao.models.User
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio._

class UserRepositoryImpl(ctx: Quill.Postgres[SnakeCase]) extends UserRepository.Service {

  import ctx._

  private val users = quote(
    querySchema[User]("users", _.id -> "id", _.email -> "email", _.name -> "name")
  )

  override def create(entity: User): Task[User] =
    run(
      quote(
        users.insert(_.email -> lift(entity.email), _.name -> lift(entity.name))
      ).returning(_.id)
    ).map(id => entity.copy(id = id))

  override def read(id: Long): Task[Option[User]] =
    run(
      quote(
        users.filter(_.id == lift(id))
      )
    ).map(_.headOption)

  override def update(entity: User): Task[Option[User]] =
    run(
      quote(
        users.updateValue(lift(entity))
      ).returning(_.id)
    ).fold(_ => None, id => Some(entity.copy(id = id)))

  override def delete(id: Long): Task[Unit] =
    run(
      quote(
        users.filter(_.id == lift(id)).delete
      )
    ).map(_ => {})
}

object UserRepositoryImpl {
  val live = ZLayer.fromFunction(new UserRepositoryImpl(_))
}