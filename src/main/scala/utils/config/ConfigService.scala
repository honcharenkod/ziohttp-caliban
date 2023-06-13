package utils.config

import utils.config
import zio.ZLayer
import zio.config._
import zio.config.magnolia._
import zio.config.typesafe._

object ConfigService {
  type Config = Service

  trait Service {
    def get: CommonConfig
  }

  case class CommonConfig(jwt: JWT)

  case class JWT(secretKey: String)

  val live: ZLayer[Any, ReadError[String], Config] = ZLayer.fromZIO {
    for {
      config <-
        read(
          descriptorForPureConfig[CommonConfig] from ConfigSource.fromHoconFilePath("src/main/resources/application.conf")
        )
    } yield new Service {
      override def get: CommonConfig = config
    }
  }
}
