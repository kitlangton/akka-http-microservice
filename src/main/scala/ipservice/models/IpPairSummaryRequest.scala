package ipservice.models

import io.circe._
import io.circe.generic.semiauto._
import zio.schema._

case class IpPairSummaryRequest(ip1: String, ip2: String)

object IpPairSummaryRequest {
  implicit val ipPairSummaryRequestDecoder: Decoder[IpPairSummaryRequest] = deriveDecoder
  implicit val ipPairSummaryRequestEncoder: Encoder[IpPairSummaryRequest] = deriveEncoder
  implicit val schema: Schema[IpPairSummaryRequest]                       = DeriveSchema.gen
}
