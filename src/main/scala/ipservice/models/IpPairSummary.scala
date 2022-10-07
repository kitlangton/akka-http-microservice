package ipservice.models

import io.circe._
import io.circe.generic.semiauto._

import scala.math._

case class IpPairSummary(distance: Option[Double], ip1Info: IpInfo, ip2Info: IpInfo)

object IpPairSummary {
  def apply(ip1Info: IpInfo, ip2Info: IpInfo): IpPairSummary =
    IpPairSummary(calculateDistance(ip1Info, ip2Info), ip1Info, ip2Info)

  private def calculateDistance(ip1Info: IpInfo, ip2Info: IpInfo): Option[Double] =
    (ip1Info.lat, ip1Info.lon, ip2Info.lat, ip2Info.lon) match {
      case (Some(lat1), Some(lon1), Some(lat2), Some(lon2)) =>
        // see http://www.movable-type.co.uk/scripts/latlong.html
        val φ1 = toRadians(lat1)
        val φ2 = toRadians(lat2)
        val Δφ = toRadians(lat2 - lat1)
        val Δλ = toRadians(lon2 - lon1)
        val a  = pow(sin(Δφ / 2), 2) + cos(φ1) * cos(φ2) * pow(sin(Δλ / 2), 2)
        val c  = 2 * atan2(sqrt(a), sqrt(1 - a))
        Option(EarthRadius * c)
      case _ => None
    }

  private val EarthRadius = 6371.0

  implicit val ipPairSummaryEncoder: Encoder[IpPairSummary] = deriveEncoder
  implicit val ipPairSummaryDecoder: Decoder[IpPairSummary] = deriveDecoder
}
