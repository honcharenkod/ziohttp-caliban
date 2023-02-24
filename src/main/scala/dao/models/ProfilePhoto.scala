package dao.models

import zio._

case class ProfilePhoto(id: Long, userId: Long, data: Chunk[Byte], mimeType: String)
