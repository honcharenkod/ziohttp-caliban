package dao.repositories

import dao.models.Message
import zio.Task

trait MessageRepository {
  def sendMessage(text: String, senderId: Long, recipientId: Long): Task[Message]
}
