package ipservice.zio

import zio.{UIO, ZIO, ZLayer}

trait StupidService {
  def duh: UIO[Unit]
}

final case class StupidServiceConfig(saying: String)

final case class StupidServiceLive(config: StupidServiceConfig) extends StupidService {
  def duh: UIO[Unit] = ZIO.debug(config.saying)
}

object StupidServiceLive {
  val layer = ZLayer.fromFunction(StupidServiceLive.apply _)
}
