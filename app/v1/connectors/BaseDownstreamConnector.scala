/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.connectors

import config.AppConfig
import play.api.Logger
import play.api.libs.json.Writes
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import v1.models.downstream.DownstreamConfig

import scala.concurrent.{ExecutionContext, Future}

trait BaseDownstreamConnector {
  val http: HttpClient
  val appConfig: AppConfig
  def downstreamService: DownstreamService

  def downstreamConfig: DownstreamConfig = downstreamService match {
    case DownstreamService.DES => DownstreamConfig(token = appConfig.desToken, environment = appConfig.desEnv, baseUrl = appConfig.desBaseUrl)
    case DownstreamService.IFS => DownstreamConfig(token = appConfig.ifsToken, environment = appConfig.ifsEnv, baseUrl = appConfig.ifsBaseUrl)
  }

  val logger: Logger = Logger(this.getClass)

  private[connectors] def downstreamHeaderCarrier(implicit hc: HeaderCarrier, correlationId: String): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer ${downstreamConfig.token}")))
      .withExtraHeaders("Environment" -> downstreamConfig.environment, "CorrelationId" -> correlationId)

  def post[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(implicit ec: ExecutionContext,
                                                                     hc: HeaderCarrier,
                                                                     httpReads: HttpReads[DownstreamOutcome[Resp]],
                                                                     correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doPost(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.POST(s"${downstreamConfig.baseUrl}/${uri.value}", body)
    }

    doPost(downstreamHeaderCarrier(hc, correlationId))
  }

  def put[Body: Writes, Resp](body: Body, uri: DownstreamUri[Resp])(implicit ec: ExecutionContext,
                                                                    hc: HeaderCarrier,
                                                                    httpReads: HttpReads[DownstreamOutcome[Resp]],
                                                                    correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doPut(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.PUT(s"${downstreamConfig.baseUrl}/${uri.value}", body)
    }

    doPut(downstreamHeaderCarrier(hc, correlationId))
  }

  def get[Resp](uri: DownstreamUri[Resp])(implicit ec: ExecutionContext,
                                          hc: HeaderCarrier,
                                          httpReads: HttpReads[DownstreamOutcome[Resp]],
                                          correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doGet(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.GET(s"${downstreamConfig.baseUrl}/${uri.value}")

    doGet(downstreamHeaderCarrier(hc, correlationId))
  }

  def delete[Resp](uri: DownstreamUri[Resp])(implicit ec: ExecutionContext,
                                             hc: HeaderCarrier,
                                             httpReads: HttpReads[DownstreamOutcome[Resp]],
                                             correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doDelete(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.DELETE(s"${downstreamConfig.baseUrl}/${uri.value}")

    doDelete(downstreamHeaderCarrier(hc, correlationId))
  }

}
