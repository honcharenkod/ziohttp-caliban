package dao.models

import caliban.schema.Schema
import io.getquill._
import zio.json._

sealed trait Role

object Role {
  case object Admin extends Role

  case object User extends Role

  implicit val quillEncoder: MappedEncoding[Role, String] = MappedEncoding[Role, String] {
    case Admin => "admin"
    case User => "user"
  }

  implicit val quillDecoder: MappedEncoding[String, Role] = MappedEncoding[String, Role] {
    case "admin" => Admin
    case "user" => User
  }

  implicit val jsonEncoder: JsonEncoder[Role] =
    DeriveJsonEncoder.gen[Role]

  implicit val jsonDecoder: JsonDecoder[Role] =
    DeriveJsonDecoder.gen[Role]

  implicit val schema: Schema[Any, Role] = Schema.gen
}

