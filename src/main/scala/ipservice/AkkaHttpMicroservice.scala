package ipservice

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.circe.{Decoder, Encoder}
import ipservice.models.{IpInfo, IpPairSummary, IpPairSummaryRequest}

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future}

trait Service extends ErrorAccumulatingCirceSupport {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContext

  def config: Config
  val logger: LoggingAdapter

  lazy val ipApiConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
    Http().outgoingConnection(config.getString("services.ip-api.host"), config.getInt("services.ip-api.port"))

  // Please note that using `Source.single(request).via(pool).runWith(Sink.head)` is considered anti-pattern. It's here only for the simplicity.
  // See why and how to improve it here: https://github.com/theiterators/akka-http-microservice/issues/32
  def ipApiRequest(request: HttpRequest): Future[HttpResponse] =
    Source.single(request).via(ipApiConnectionFlow).runWith(Sink.head)

  def fetchIpInfo(ip: String): Future[Either[String, IpInfo]] =
    ipApiRequest(RequestBuilding.Get(s"/json/$ip")).flatMap { response =>
      response.status match {
        case OK         => Unmarshal(response.entity).to[IpInfo].map(Right(_))
        case BadRequest => Future.successful(Left(s"$ip: incorrect IP format"))
        case _ =>
          Unmarshal(response.entity).to[String].flatMap { entity =>
            val error = s"FreeGeoIP request failed with status code ${response.status} and entity $entity"
            logger.error(error)
            Future.failed(new IOException(error))
          }
      }
    }

  // - GET  /ip/{some.ip.address}
  // - POST /ip with Body[IpPairSummaryRequest]
  val routes: Route =
    logRequestResult("akka-http-microservice") {
      pathPrefix("ip") {
        (get & path(Segment)) { ip =>
          complete {
            fetchIpInfo(ip).map[ToResponseMarshallable] {
              case Right(ipInfo)      => ipInfo
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
          (post & entity(as[IpPairSummaryRequest])) { ipPairSummaryRequest =>
            complete {
              val ip1InfoFuture = fetchIpInfo(ipPairSummaryRequest.ip1)
              val ip2InfoFuture = fetchIpInfo(ipPairSummaryRequest.ip2)
              ip1InfoFuture.zip(ip2InfoFuture).map[ToResponseMarshallable] {
                case (Right(info1), Right(info2)) => IpPairSummary.apply(info1, info2)
                case (Left(errorMessage), _)      => BadRequest -> errorMessage
                case (_, Left(errorMessage))      => BadRequest -> errorMessage
              }
            }
          }
      }
    }
}

object AkkaHttpMicroservice extends App with Service {
  override implicit val system: ActorSystem        = ActorSystem()
  override implicit val executor: ExecutionContext = system.dispatcher

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().newServerAt(config.getString("http.interface"), config.getInt("http.port")).bindFlow(routes)
}
