package ipservice.zio

import zio.ULayer
import zio.config.typesafe.TypesafeConfig

final case class Configuration(
    stupid: StupidServiceConfig,
    ip: IpServiceConfig
)

object Configuration {
  private val descriptor = zio.config.magnolia.descriptor[Configuration]

  val baseLayer: ULayer[Configuration] =
    TypesafeConfig.fromResourcePath(descriptor).orDie

  val layer: ULayer[StupidServiceConfig with IpServiceConfig with Configuration] =
    baseLayer.project(_.stupid) ++
      baseLayer.project(_.ip) ++
      baseLayer
}
