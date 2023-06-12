package dao.models

import caliban.schema.Schema

case class Message(id: Long, text: String, senderId: Long, recipientId: Long)

object Message {
  implicit val schema: Schema[Any, Message] = Schema.gen
}
