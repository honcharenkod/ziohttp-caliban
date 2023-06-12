package dao.models

case class Message(id: Long, text: String, senderId: Long, recipientId: Long)
