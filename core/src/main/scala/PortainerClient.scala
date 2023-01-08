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

import cats.effect.Concurrent
import io.circe.Decoder
import io.circe.Encoder
import io.circe.syntax._
import org.http4s._
import org.http4s.headers.Accept
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

trait PortainerClient[F[_]] {
  def send[I: Encoder, O: Decoder](
      uri: Uri => Uri,
      method: Method,
      body: I
  ): F[O]

  def send[O: Decoder](
      uri: Uri => Uri,
      method: Method
  ): F[O]

  final def get[O: Decoder](base: Uri => Uri)(queries: (String, String)*) =
    send(base.andThen(_.withQueryParams(queries.toMap)), Method.GET)
}

object PortainerClient {

  def printer[F[_]](
      base: Uri,
      cred: PortainerCredential = PortainerCredential.Public
  ): PortainerClient[Printed[F, *]] =
    new PortainerClient[Printed[F, *]] with Http4sClientDsl[F] {

      override def send[I: Encoder, O: Decoder](
          uri: Uri => Uri,
          method: Method,
          body: I
      ): Printed[F, O] = Printed(
        method(uri(base)).andHeader(cred.toHeader),
        Some(body.asJson)
      )

      override def send[O: Decoder](
          uri: Uri => Uri,
          method: Method
      ): Printed[F, O] = Printed(
        method(uri(base)).andHeader(cred.toHeader),
        None
      )

    }

  def apply[F[_]: Concurrent](
      base: Uri,
      client: Client[F],
      cred: PortainerCredential = PortainerCredential.Public
  ): PortainerClient[F] =
    new PortainerClient[F] with Http4sClientDsl[F] {
      import org.http4s.circe.CirceEntityCodec._

      override def send[I: Encoder, O: Decoder](
          uri: Uri => Uri,
          method: Method,
          body: I
      ): F[O] = client.expect(
        method(body = body, uri = uri(base)).andHeader(cred.toHeader)
      )

      override def send[O: Decoder](
          uri: Uri => Uri,
          method: Method
      ): F[O] = client.expect(
        method(uri = uri(base)).andHeader(cred.toHeader)
      )

    }

  private implicit class RichRequestHeaders[F[_]](val request: Request[F])
      extends AnyVal {
    def andHeader(headers: Header.ToRaw*): Request[F] =
      request.withHeaders(
        request.headers ++ Headers(headers: _*).add(
          Accept(MediaType.application.json)
        )
      )
  }
}

sealed trait PortainerCredential extends Serializable with Product {
  def toHeader: Headers
}
object PortainerCredential {
  final case class Login(token: String) extends PortainerCredential {
    override def toHeader: Headers = Headers(
      "Authorization" -> s"Bearer $token"
    )
  }
  final case class Token(value: String) extends PortainerCredential {
    override def toHeader: Headers = Headers("x-api-key" -> value)
  }
  case object Public extends PortainerCredential {
    override def toHeader: Headers = Headers.empty
  }

}
