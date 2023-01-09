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

import io.circe._
import io.circe.syntax._

import java.net.URI

object Sessions {
  implicit val encoder: Encoder[Sessions] =
    Encoder.instance(s => Json.obj("servers" -> s.servers.asJson))
  implicit val decoder: Decoder[Sessions] = (c: HCursor) =>
    c.downField("servers").as[Map[ServerName, Session]].map(Sessions(_))
}

final case class Sessions(
    servers: Map[ServerName, Session]
)

final case class Session(
    address: URI,
    token: LoginToken
)

object Session {
  implicit val encoder: Encoder[Session] =
    Encoder.instance(s =>
      Json.obj("address" -> s.address.asJson, "token" -> s.token.asJson)
    )
  implicit val decoder: Decoder[Session] = (c: HCursor) =>
    for {
      ad <- c.downField("address").as[URI]
      tk <- c.downField("token").as[String]
    } yield Session(ad, tk)
}
