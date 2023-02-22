package dao.repositories

import dao.models.Message
import zio.Task

object MessageRepository {

  type MessageRepository = MessageRepository.Service

  trait Service {
   def sendMessage(text: String, senderId: Long, recipientId: Long): Task[Message]
  }

}
