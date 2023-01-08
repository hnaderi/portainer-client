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

import models._

trait ResultPrinter[O] {
  def print(o: O): String
}

object ResultPrinter {
  implicit def apply[O](implicit rp: ResultPrinter[O]): ResultPrinter[O] = rp

  implicit val jsonPrinter: ResultPrinter[Json] = _.noSpaces
  implicit val stackPrinter: ResultPrinter[Stack] = _ => ""
  implicit val endpointPrinter: ResultPrinter[Endpoint] = _ => ""
}
