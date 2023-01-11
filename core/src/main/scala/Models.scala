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

package dev.hnaderi.portainer.models

import io.circe._
import io.circe.syntax._

final case class LoginToken(jwt: String) extends AnyVal
object LoginToken {
  implicit val decoder: Decoder[LoginToken] = (c: HCursor) =>
    c.downField("jwt").as[String].map(LoginToken(_))
}

final case class Endpoint(id: Int, name: String, tagIds: List[String])
object Endpoint {
  implicit val decoder: Decoder[Endpoint] = (c: HCursor) =>
    for {
      id <- c.downField("Id").as[Int]
      name <- c.downField("Name").as[String]
      tagIds <- c.downField("TagIds").as[List[String]]
    } yield Endpoint(id, name, tagIds)
}

final case class Stack(
    id: Int,
    name: String,
    swarmId: Option[String],
    endpointId: String
)
object Stack {
  implicit val decoder: Decoder[Stack] = (c: HCursor) =>
    for {
      id <- c.downField("Id").as[Int]
      name <- c.downField("Name").as[String]
      swarmId <- c.downField("SwarmId").as[Option[String]]
      endpointId <- c.downField("EndpointId").as[String]
    } yield Stack(id, name, swarmId, endpointId)
}

final case class StackCreateRequest(
    name: String,
    env: Map[String, String],
    compose: String,
    swarmId: String
)
object StackCreateRequest {
  implicit val encoder: Encoder[StackCreateRequest] = Encoder.instance(req =>
    Json.obj(
      "Name" -> req.name.asJson,
      "Env" -> req.env.asJson,
      "StackFileContent" -> req.compose.base64.asJson,
      "SwarmID" -> req.swarmId.asJson
    )
  )
}
final case class StackUpdateRequest(
    id: Int,
    env: Map[String, String],
    compose: String,
    prune: Boolean
)
object StackUpdateRequest {
  implicit val encoder: Encoder[StackUpdateRequest] = Encoder.instance(req =>
    Json.obj(
      "Id" -> req.id.asJson,
      "Env" -> req.env.asJson,
      "StackFileContent" -> req.compose.base64.asJson,
      "Prune" -> req.prune.asJson
    )
  )
}

final case class ConfigCreateRequest(
    name: String,
    labels: Map[String, String],
    data: String
)
object ConfigCreateRequest {
  implicit val encoder: Encoder[ConfigCreateRequest] = Encoder.instance(req =>
    Json.obj(
      "Name" -> req.name.asJson,
      "Labels" -> req.labels.asJson,
      "Data" -> req.data.base64.asJson
    )
  )
}

final case class SecretCreateRequest(
    name: String,
    labels: Map[String, String],
    data: String
)
object SecretCreateRequest {
  implicit val encoder: Encoder[SecretCreateRequest] = Encoder.instance(req =>
    Json.obj(
      "Name" -> req.name.asJson,
      "Labels" -> req.labels.asJson,
      "Data" -> req.data.base64.asJson
    )
  )
}

final case class Config(id: String, name: String)
object Config {
  implicit val decoder: Decoder[Config] = (c: HCursor) =>
    for {
      id <- c.downField("ID").as[String]
      name <- c.downField("spec").downField("name").as[String]
    } yield Config(id, name)
}

final case class Secret(id: String, name: String)
object Secret {
  implicit val decoder: Decoder[Secret] = (c: HCursor) =>
    for {
      id <- c.downField("ID").as[String]
      name <- c.downField("spec").downField("name").as[String]
    } yield Secret(id, name)
}
