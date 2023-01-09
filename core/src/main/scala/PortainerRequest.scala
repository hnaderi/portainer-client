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

import io.circe.Json
import io.circe.Decoder
import cats.implicits._
import cats.MonadThrow

trait PortainerRequest[O] {
  def callRaw[F[_]](client: PortainerClient[F]): F[Json]
  def call[F[_]: MonadThrow](client: PortainerClient[F]): F[O]
}

abstract class PortainerRequestBase[O: Decoder] extends PortainerRequest[O] {
  def callRaw[F[_]](client: PortainerClient[F]): F[Json]
  final def call[F[_]: MonadThrow](client: PortainerClient[F]): F[O] =
    callRaw(client).flatMap(j => MonadThrow[F].fromEither(j.as[O]))
}

abstract class PortainerRequestRaw extends PortainerRequest[Json] {
  final def call[F[_]: MonadThrow](client: PortainerClient[F]): F[Json] =
    callRaw(client)
}
