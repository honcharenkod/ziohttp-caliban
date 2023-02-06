package dao

import dao.UserDaoImpl.live
import dao.models.User
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.{Task, ZLayer}

class UserDaoImpl(ctx: Quill.Postgres[SnakeCase]) extends CommonDAO[User] {

  import ctx._

  override val quoted = quote(
    querySchema[User]("users", _.id -> "id", _.email -> "email", _.name -> "name", _.surname -> "surname")
  )
  override def create(entity: User): Task[User] =
    run(
      quote(
        quoted.insert(_.email -> lift(entity.email), _.name -> lift(entity.name), _.surname -> lift(entity.surname))
      ).returning(_.id)
    ).map(id => entity.copy(id = id))

  override def read(id: Long): Task[Option[User]] =
    run(
      quote(
        quoted.filter(_.id == lift(id))
      )
    ).map(_.headOption)

  override def update(entity: User): Task[Option[User]] =
    run(
      quote(
        quoted.updateValue(lift(entity))
      ).returning(_.id)
    ).fold(_ => None, id => Some(entity.copy(id = id)))

  override def delete(id: Long): Task[Unit] =
    run(
      quote(
        quoted.filter(_.id == lift(id)).delete
      )
    ).map(_ => {})

}

object UserDaoImpl {
  val live = ZLayer.fromFunction(new UserDaoImpl(_))
}
