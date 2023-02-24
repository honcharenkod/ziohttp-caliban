package dao

import dao.models.ProfilePhoto
import io.getquill.{EntityQuery, Quoted, SnakeCase}
import io.getquill.jdbczio.Quill
import zio.{Task, ZLayer}

class ProfilePhotoDAOImpl(ctx: Quill.Postgres[SnakeCase]) extends CommonDAO[ProfilePhoto] {

  import ctx._

  override val quoted: Quoted[EntityQuery[ProfilePhoto]] =
    quote(
      querySchema[ProfilePhoto](
        "profile_photos",
        _.id -> "id",
        _.userId -> "user_id",
        _.data -> "data",
        _.mimeType -> "mime_type"
      )
    )

  override def create(entity: ProfilePhoto): Task[ProfilePhoto] =
    run(
      quote(
        quoted.insert(
          _.id -> lift(entity.id),
          _.userId -> lift(entity.id),
          _.data -> lift(entity.data),
          _.mimeType -> lift(entity.mimeType)
        )
      ).returning(_.id)
    ).map(id => entity.copy(id = id))

  override def read(id: Long): Task[Option[ProfilePhoto]] =
    run(
      quote(
        quoted.filter(_.id == lift(id))
      )
    ).map(_.headOption)

  override def update(entity: ProfilePhoto): Task[Option[ProfilePhoto]] =
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

object ProfilePhotoDAOImpl {
  val live = ZLayer.fromFunction(new ProfilePhotoDAOImpl(_))
}

