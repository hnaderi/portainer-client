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

import cats.data.NonEmptyList
import cats.implicits._

sealed trait CLIError extends Throwable

object CLIError {
  final case class UnknownServerSession(name: String)
      extends Exception(s"'$name' is not a known server session")
      with CLIError
  final case class InvalidEnvFile(errors: NonEmptyList[String])
      extends Exception(
        s"Invalid env file!\n${errors.mkString_("\n")}"
      )
      with CLIError
  final case class InvalidEndpointSelector(
      selector: EndpointSelector,
      size: Int
  ) extends Exception(
        s"You must provide an endpoint selector that selects exactly one endpoint, but I found $size endpoints using `$selector`"
      )
      with CLIError

}
