package utils.config

import zio.config._
import typesafe._
import magnolia._
import zio.ZLayer

object ConfigService {
  trait ConfigService {
    def get: CommonConfig
  }

  case class ConfigServiceImpl(config: CommonConfig) extends ConfigService {
    override def get: CommonConfig = config
  }

  case class CommonConfig(jwt: JWT)
  case class JWT(secretKey: String)

  val live = ZLayer {
    for {
      config <-
        read(
          descriptorForPureConfig[CommonConfig] from ConfigSource.fromHoconFilePath("src/main/resources/application.conf")
        )
    } yield ConfigServiceImpl(config)
  }

}
