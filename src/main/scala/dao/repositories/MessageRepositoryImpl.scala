package dao.repositories

import dao.MessageDAOImpl
import dao.models.Message
import zio._

class MessageRepositoryImpl (messageDAO: MessageDAOImpl) extends MessageRepository {
  override def sendMessage(text: String, senderId: Long, recipientId: Long): Task[Message] =
    messageDAO.create(
      Message(
        0,
        text,
        senderId,
        recipientId
      )
    )
}

object MessageRepositoryImpl {
  val live = ZLayer {
    for {
      messageDAO <- ZIO.service[MessageDAOImpl]
    } yield new MessageRepositoryImpl(messageDAO)
  }
}
