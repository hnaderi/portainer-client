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

import io.circe.Decoder
import io.circe.HCursor

final case class LoginToken(jwt: String) extends AnyVal
object LoginToken {
  implicit val decoder: Decoder[LoginToken] = (c: HCursor) =>
    c.downField("jwt").as[String].map(LoginToken(_))
}

final case class Endpoint()
object Endpoint {
  implicit val decoder: Decoder[Endpoint] = Decoder.const(Endpoint())
}

final case class EndpointGroup()
object EndpointGroup {
  implicit val decoder: Decoder[EndpointGroup] =
    Decoder.const(EndpointGroup())
}

final case class Registry()
object Registry {
  implicit val decoder: Decoder[Registry] = Decoder.const(Registry())
}

final case class Stack()
object Stack {
  implicit val decoder: Decoder[Stack] = Decoder.const(Stack())
}
