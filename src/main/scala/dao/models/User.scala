package dao.models

import zio.json._

case class User(id: Long, email: String, name: String)

object User {
  implicit val decoder: JsonDecoder[User] =
    DeriveJsonDecoder.gen[User]

  implicit val encoder: JsonEncoder[User] =
    DeriveJsonEncoder.gen[User]
}