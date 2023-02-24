package dao

import dao.models.Message
import io.getquill.jdbczio.Quill
import io.getquill.{EntityQuery, Quoted, SnakeCase}
import zio.{Task, ZLayer}

class MessageDAOImpl(ctx: Quill.Postgres[SnakeCase]) extends CommonDAO[Message] {

  import ctx._

  override val quoted: Quoted[EntityQuery[Message]] =
    quote(
      querySchema[Message](
        "messages",
        _.id -> "id",
        _.text -> "text",
        _.senderId -> "sender_id",
        _.recipientId -> "recipient_id"
      )
    )

  override def create(entity: Message): Task[Message] =
    run(
      quote(
        quoted.insert(
          _.text -> lift(entity.text),
          _.senderId -> lift(entity.senderId),
          _.recipientId -> lift(entity.recipientId)
        )
      ).returning(_.id)
    ).map(id => entity.copy(id = id))

  override def read(id: Long): Task[Option[Message]] =
    run(
      quote(
        quoted.filter(_.id == lift(id))
      )
    ).map(_.headOption)

  override def update(entity: Message): Task[Option[Message]] =
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

object MessageDAOImpl {
  val live = ZLayer.fromFunction(new MessageDAOImpl(_))
}

