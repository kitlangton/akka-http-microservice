package ipservice.zio

import akka.actor.ActorSystem
import ipservice.models.{IpInfo, IpPairSummary, IpPairSummaryRequest}
import zio._
import zio.http._
import zio.http.api._

final case class ZioHttpMicroservice(stupidService: StupidService, ipService: IpService) {

  val singleIp =
    API
      .get("ip" / In.string)
      .output[IpInfo]
      .handle { ip =>
        ipService.fetchIpInfo(ip)
      }

  val stupidApi =
    API
      .get("stupid")
      .handle { _ =>
        stupidService.duh
      }

  val ipDistance =
    API
      .post("ip")
      .input[IpPairSummaryRequest]
      .output[IpPairSummary]
      .handle { request =>
        for {
          results <- ipService
                       .fetchIpInfo(request.ip1)
                       .zipPar(ipService.fetchIpInfo(request.ip2))
        } yield IpPairSummary(results._1, results._2)
      }

  val app = (singleIp ++ ipDistance ++ stupidApi).toHttpApp

  def start: URIO[Server, Nothing] =
    Server.serve(app)
}

object ZioHttpMicroservice {
  val live =
    ZLayer.fromFunction(ZioHttpMicroservice.apply _)
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

  val run =
    ZIO
      .serviceWithZIO[ZioHttpMicroservice](_.start)
      .provide(
        ZLayer.Debug.mermaid,
        ZioHttpMicroservice.live,
        IpServiceAkka.layer,
        actorSystemLayer,
        StupidServiceLive.layer,
        Configuration.layer,
        Server.default
      )
}
