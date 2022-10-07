package ipservice.zio

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http => AkkaHttp}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import ipservice.models.IpInfo
import zio._

import java.io.IOException

trait IpService {
  def fetchIpInfo(ip: String): Task[Either[String, IpInfo]]
}

final case class IpServiceAkka(config: Config, actorSystem: ActorSystem)
    extends IpService
    with ErrorAccumulatingCirceSupport {

  implicit private val system   = actorSystem
  implicit private val executor = actorSystem.dispatcher

  lazy val ipApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    AkkaHttp().outgoingConnection(
      config.getString("services.ip-api.host"),
      config.getInt("services.ip-api.port")
    )

  // Please note that using `Source.single(request).via(pool).runWith(Sink.head)` is considered anti-pattern. It's here only for the simplicity.
  // See why and how to improve it here: https://github.com/theiterators/akka-http-microservice/issues/32
  def ipApiRequest(request: HttpRequest): IO[Throwable, HttpResponse] =
    ZIO.fromFuture { _ =>
      Source.single(request).via(ipApiConnectionFlow).runWith(Sink.head)
    }

  def fetchIpInfo(ip: String): IO[Throwable, Either[String, IpInfo]] =
    ipApiRequest(RequestBuilding.Get(s"/json/$ip")).flatMap { response =>
      response.status match {
        case OK         => ZIO.fromFuture(_ => Unmarshal(response.entity).to[IpInfo].map(Right(_)))
        case BadRequest => ZIO.succeed(Left(s"$ip: incorrect IP format"))
        case _ =>
          ZIO.fromFuture(_ => Unmarshal(response.entity).to[String]).flatMap { entity =>
            val error = s"FreeGeoIP request failed with status code ${response.status} and entity $entity"
            ZIO.logError(error) *>
              ZIO.fail(new IOException(error))
          }
      }
    }
}

object IpServiceAkka {
  val layer: ZLayer[Config with ActorSystem, Nothing, IpService] =
    ZLayer.fromFunction(IpServiceAkka(_, _))
}
