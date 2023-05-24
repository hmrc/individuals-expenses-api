/*
 * Copyright 2023 HM Revenue & Customs
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

package api.controllers.requestValidators

import utils.Logging
import api.models.errors.{BadRequestError, ErrorWrapper}
import api.models.request.RawData
import api.models.errors.MtdError

trait RequestValidator[Raw <: RawData, Request] extends Logging {

  def parseRequest(data: Raw)(implicit correlationId: String): Either[ErrorWrapper, Request] = {
    validate(data) match {
      case Nil =>
        logger.info(
          message = "[RequestValidator][parseRequest] " +
            s"Validation successful for the request with correlationId : $correlationId")
        Right(requestFor(data))
      case err :: Nil =>
        logger.warn(
          message = "[RequestValidator][parseRequest] " +
            s"Validation failed with ${err.code} error for the request with correlationId : $correlationId")
        Left(ErrorWrapper(correlationId, err, None))
      case errs =>
        logger.warn(
          "[RequestValidator][parseRequest] " +
            s"Validation failed with ${errs.map(_.code).mkString(",")} error for the request with correlationId : $correlationId")
        Left(ErrorWrapper(correlationId, BadRequestError, Some(errs)))
    }
  }

  protected def validate(data: Raw): List[MtdError] =
    run(validationSet, data) match {
      case Some(errors) => errors
      case None         => Nil
    }

  protected def validationSet: List[Raw => List[List[MtdError]]]

  protected def requestFor(data: Raw): Request

  private def run(validationSet: List[Raw => List[List[MtdError]]], data: Raw): Option[List[MtdError]] = {
    validationSet.foldLeft(Option.empty[List[MtdError]]) { (result, validation) =>
      result match {
        case Some(errors) => Some(errors)
        case None =>
          val errors = validation(data).flatten
          if (errors.nonEmpty) Some(errors) else None
      }
    }
  }

}

object RequestValidator {

  def flattenErrors(errors: List[List[MtdError]]): List[MtdError] = {
    errors.flatten
      .groupBy(_.message)
      .map { case (_, errors) =>
        val baseError = errors.head.copy(paths = Some(Seq.empty[String]))

        errors.fold(baseError)((error1, error2) => {
          val paths: Option[Seq[String]] = for {
            error1Paths <- error1.paths
            error2Paths <- error2.paths
          } yield {
            error1Paths ++ error2Paths
          }
          error1.copy(paths = paths)
        })
      }
      .toList
  }

}
