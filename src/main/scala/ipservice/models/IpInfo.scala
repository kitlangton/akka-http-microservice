package ipservice.models

import io.circe._
import io.circe.generic.semiauto._

case class IpInfo(
    query: String,
    country: Option[String],
    city: Option[String],
    lat: Option[Double],
    lon: Option[Double]
)

object IpInfo {
  implicit val ipInfoDecoder: Decoder[IpInfo] = deriveDecoder
  implicit val ipInfoEncoder: Encoder[IpInfo] = deriveEncoder
}
