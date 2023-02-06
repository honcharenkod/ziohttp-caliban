package dao

import dao.models.AuthInfo
import io.getquill.jdbczio.Quill
import io.getquill.{EntityQuery, Quoted, SnakeCase}
import zio.{Task, ZLayer}

class AuthInfoDAOImpl(ctx: Quill.Postgres[SnakeCase]) extends CommonDAO[AuthInfo] {

  import ctx._

  override val quoted: Quoted[EntityQuery[AuthInfo]] =
    quote(
      querySchema[AuthInfo]("auth_info", _.userId -> "user_id", _.hashedPassword -> "hashed_password")
    )

  override def create(entity: AuthInfo): Task[AuthInfo] =
    run(
      quote(
        quoted.insert(_.userId -> lift(entity.userId), _.hashedPassword -> lift(entity.hashedPassword))
      )
    ).map(_ => entity)

  override def read(id: Long): Task[Option[AuthInfo]] =
    run(
      quote(
        quoted.filter(_.id == lift(id))
      )
    ).map(_.headOption)

  override def update(entity: AuthInfo): Task[Option[AuthInfo]] =
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

object AuthInfoDAOImpl {
  val live = ZLayer.fromFunction(new AuthInfoDAOImpl(_))
}
