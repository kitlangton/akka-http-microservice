package ipservice.zio

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.syntax.EncoderOps
import ipservice.models.{IpInfo, IpPairSummary, IpPairSummaryRequest}
import zhttp.http._
import zhttp.service._
import zio._

final case class ZioHttpMicroservice(ipService: IpService) {

  val app = Http.collectZIO[Request] {
    case Method.GET -> !! / "ip" / ip =>
      for {
        info <- ipService.fetchIpInfo(ip)
        response = info match {
                     case Left(error) =>
                       Response.text(error).setStatus(Status.BadRequest)
                     case Right(info) =>
                       Response.json(info.asJson.noSpaces)
                   }
      } yield response

    case req @ Method.POST -> !! / "ip" =>
      for {
        bodyString <- req.body.asString
        summary    <- ZIO.fromEither(bodyString.asJson.as[IpPairSummaryRequest])
        results    <- ipService.fetchIpInfo(summary.ip1).zipPar(ipService.fetchIpInfo(summary.ip2))
        response = results match {
                     case (Right(info1), Right(info2)) =>
                       Response.json(IpPairSummary(info1, info2).asJson.noSpaces)
                     case (Left(error), _) =>
                       Response.text(error).setStatus(Status.BadRequest)
                     case (_, Left(error)) =>
                       Response.text(error).setStatus(Status.BadRequest)
                   }
      } yield response
  }

  def start: ZIO[Any, Throwable, Nothing] =
    Server.start(8080, app)
}

object ZioHttpMicroservice {
  val live: ZLayer[IpService, Nothing, ZioHttpMicroservice] =
    ZLayer.fromFunction(ZioHttpMicroservice(_))
}

object ZioMain extends ZIOAppDefault {

  val actorSystemLayer: ULayer[ActorSystem] =
    ZLayer.scoped {
      ZIO.acquireRelease {
        ZIO.succeed(ActorSystem())
      } { actorSystem =>
        ZIO.succeed(actorSystem.terminate())
      }
    }

  val configLayer: ULayer[Config] =
    ZLayer.succeed(ConfigFactory.load())

  val run =
    ZIO
      .serviceWithZIO[ZioHttpMicroservice](_.start)
      .provide(
        ZLayer.Debug.mermaid,
        ZioHttpMicroservice.live,
        IpServiceAkka.layer,
        actorSystemLayer,
        configLayer
      )
}
