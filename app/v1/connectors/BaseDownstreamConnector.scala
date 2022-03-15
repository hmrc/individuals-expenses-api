/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}

import scala.concurrent.{ExecutionContext, Future}

trait BaseDownstreamConnector {
  val http: HttpClient
  val appConfig: AppConfig

  private def desHeaderCarrier(additionalHeaders: Seq[String])(implicit hc: HeaderCarrier,
                                                               correlationId: String): HeaderCarrier =
    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${appConfig.desToken}",
          "Environment" -> appConfig.desEnvironment,
          "CorrelationId" -> correlationId
        ) ++
        // Other headers (i.e Gov-Test-Scenario, Content-Type)
        hc.headers(additionalHeaders ++ appConfig.desEnvironmentHeaders.getOrElse(Seq.empty))
    )

  private def ifsR5HeaderCarrier(additionalHeaders: Seq[String])(implicit hc: HeaderCarrier,
                                                                 correlationId: String): HeaderCarrier =
    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${appConfig.ifsR5Token}",
          "Environment" -> appConfig.ifsR5Environment,
          "CorrelationId" -> correlationId
        ) ++
        // Other headers (i.e Gov-Test-Scenario, Content-Type)
        hc.headers(additionalHeaders ++ appConfig.ifsR5EnvironmentHeaders.getOrElse(Seq.empty))
    )


  private def ifsR6HeaderCarrier(additionalHeaders: Seq[String])(implicit hc: HeaderCarrier,
                                                                 correlationId: String): HeaderCarrier =
    HeaderCarrier(
      extraHeaders = hc.extraHeaders ++
        // Contract headers
        Seq(
          "Authorization" -> s"Bearer ${appConfig.ifsR6Token}",
          "Environment" -> appConfig.ifsR6Environment,
          "CorrelationId" -> correlationId
        ) ++
        // Other headers (i.e Gov-Test-Scenario, Content-Type)
        hc.headers(additionalHeaders ++ appConfig.ifsR6EnvironmentHeaders.getOrElse(Seq.empty))
    )

  def post[Body: Writes, Resp](body: Body, request: DownstreamRequest[Resp])(implicit ec: ExecutionContext,
                                                                             hc: HeaderCarrier,
                                                                             httpReads: HttpReads[DownstreamOutcome[Resp]],
                                                                             correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doPost(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.POST(url = getBackendUri(request), body)
    }

    doPost(getBackendHeaders(request, hc, correlationId, Seq("Content-Type")))
  }

  def put[Body: Writes, Resp](body: Body, request: DownstreamRequest[Resp])(implicit ec: ExecutionContext,
                                                                            hc: HeaderCarrier,
                                                                            httpReads: HttpReads[DownstreamOutcome[Resp]],
                                                                            correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doPut(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] = {
      http.PUT(url = getBackendUri(request), body)
    }

    doPut(getBackendHeaders(request, hc, correlationId, Seq("Content-Type")))
  }

  def get[Resp](request: DownstreamRequest[Resp])(implicit ec: ExecutionContext,
                                                  hc: HeaderCarrier,
                                                  httpReads: HttpReads[DownstreamOutcome[Resp]],
                                                  correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doGet(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.GET(url = getBackendUri(request))

    doGet(getBackendHeaders(request, hc, correlationId))
  }

  def delete[Resp](request: DownstreamRequest[Resp])(implicit ec: ExecutionContext,
                                                     hc: HeaderCarrier,
                                                     httpReads: HttpReads[DownstreamOutcome[Resp]],
                                                     correlationId: String): Future[DownstreamOutcome[Resp]] = {

    def doDelete(implicit hc: HeaderCarrier): Future[DownstreamOutcome[Resp]] =
      http.DELETE(url = getBackendUri(request))

    doDelete(getBackendHeaders(request, hc, correlationId))
  }

  private def getBackendUri[Resp](request: DownstreamRequest[Resp]): String = request match {
    case DownstreamRequest(Des, value)   => s"${appConfig.desBaseUrl}/$value"
    case DownstreamRequest(IfsR5, value) => s"${appConfig.ifsR5BaseUrl}/$value"
    case DownstreamRequest(IfsR6, value) => s"${appConfig.ifsR6BaseUrl}/$value"
  }

  private def getBackendHeaders[Resp](request: DownstreamRequest[Resp],
                                      hc: HeaderCarrier,
                                      correlationId: String,
                                      additionalHeaders: Seq[String] = Seq.empty): HeaderCarrier =
    request match {
      case DownstreamRequest(Des, _)   => desHeaderCarrier(additionalHeaders)(hc, correlationId)
      case DownstreamRequest(IfsR5, _) => ifsR5HeaderCarrier(additionalHeaders)(hc, correlationId)
      case DownstreamRequest(IfsR6, _) => ifsR6HeaderCarrier(additionalHeaders)(hc, correlationId)
  }
}