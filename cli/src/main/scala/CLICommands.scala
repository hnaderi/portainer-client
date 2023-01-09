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

import java.net.URI

sealed trait CLICommand extends Serializable with Product
object CLICommand {
  final case class External(
      request: PortainerRequest[?],
      server: ServerConfig,
      print: Boolean = false
  ) extends CLICommand

  final case class Deploy() extends CLICommand
  final case class Destroy() extends CLICommand
  final case class CleanUp() extends CLICommand

  final case class Login(
      server: String,
      address: URI,
      username: String,
      password: Option[String] = None,
      print: Boolean = false
  ) extends CLICommand
  final case class Logout(server: String) extends CLICommand
}
