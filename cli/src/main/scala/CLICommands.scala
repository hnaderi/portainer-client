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
import cats.data.ValidatedNel
import cats.implicits._

import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

sealed trait CLICommand extends Serializable with Product
object CLICommand {
  final case class External(
      request: PortainerRequest[?],
      server: ServerConfig,
      print: Boolean = false
  ) extends CLICommand

  final case class Play(server: ServerConfig, playbook: Playbook)
      extends CLICommand

  final case class Login(
      server: String,
      address: URI,
      username: String,
      password: Option[String] = None,
      print: Boolean = false
  ) extends CLICommand
  final case class Logout(server: String) extends CLICommand
}

sealed trait Playbook extends Serializable with Product
object Playbook {
  final case class Deploy(
      compose: Path,
      endpoint: NonEmptyList[String],
      stack: String,
      env: Option[Path] = None,
      inlineVars: Option[NonEmptyList[InlineEnv]] = None,
      configs: Option[NonEmptyList[FileMapping]] = None,
      secrets: Option[NonEmptyList[FileMapping]] = None
  ) extends Playbook

  final case class Destroy(
      endpoint: NonEmptyList[String],
      stacks: NonEmptyList[String],
      configs: Option[NonEmptyList[String]] = None,
      secrets: Option[NonEmptyList[String]] = None
  ) extends Playbook
}

final case class InlineEnv(key: String, value: String)
object InlineEnv {
  private val pattern = "(.+)=(.+)".r
  def apply(str: String): Option[InlineEnv] = str match {
    case pattern(key, value) => Some(InlineEnv(key.trim, value))
    case _                   => None
  }
  def validate(str: String): ValidatedNel[String, InlineEnv] =
    apply(str).toValidNel(s"'$str' is not in valid format key=value")
}
final case class FileMapping(source: Path, name: String)
object FileMapping {
  private val pattern = "(.+):(.+)".r
  def apply(str: String): Option[FileMapping] = str match {
    case pattern(file, name) =>
      Some(FileMapping(Paths.get(file.trim), name.trim))
    case _ => None
  }
  def validate(str: String): ValidatedNel[String, FileMapping] =
    apply(str).toValidNel(s"'$str' is not in valid format file:resource")
}
