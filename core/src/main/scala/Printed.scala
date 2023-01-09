/*
 * Copyright 2023 Hossein Naderi
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

package dev.hnaderi.portainer

import cats.Show
import io.circe.Json
import io.circe.syntax._
import org.http4s._
import org.typelevel.ci.CIString

final case class Printed[F[_], O](req: Request[F], body: Option[Json] = None) {
  override def toString = Printed.requestToCurl(req, body)
}

object Printed {
  implicit def showInstance[F[_], O]: Show[Printed[F, O]] =
    Show.show(_.toString)

  // escapes characters that are used in the curl-command, such as '
  private def escapeQuotationMarks(s: String) = s.replaceAll("'", """'\\''""")

  private def newline: String = " \\\n  "

  private def prepareMethodName(method: Method): String =
    s"$newline--request ${method.name}"

  private def prepareUri(uri: Uri): String =
    s"$newline--url '${escapeQuotationMarks(uri.renderString)}'"

  private def prepareBody(uri: Json): String =
    s"$newline--data '${escapeQuotationMarks(uri.noSpaces)}'"

  private def prepareHeaders(
      headers: Headers,
      redactHeadersWhen: CIString => Boolean
  ): String = {
    val preparedHeaders = headers
      .redactSensitive(redactHeadersWhen)
      .headers
      .map { header =>
        s"""--header '${escapeQuotationMarks(
            s"${header.name}: ${header.value}"
          )}'"""
      }
      .mkString(newline)

    if (preparedHeaders.isEmpty) "" else newline + preparedHeaders
  }

  private def requestToCurl[F[_]](
      request: Request[F],
      body: Option[Json]
  ): String = {
    val params = (
      prepareHeaders(request.headers, _ => false) ::
        prepareMethodName(request.method) ::
        prepareUri(request.uri) ::
        body.map(_.asJson).map(prepareBody).toList
    ).mkString

    s"curl$params"
  }
}
