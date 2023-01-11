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

import cats.effect._
import cats.implicits._
import dev.hnaderi.readpassword

import java.nio.file.Paths

object Main extends Platform {
  private val portainerHome = sys.env.getOrElse("PORTAINER_HOME", "")
  private val sessionFile = sys.env
    .get("PORTAINER_SESSION")
    .map(Paths.get(_))
    .getOrElse(
      Paths
        .get(portainerHome)
        .resolve(".portainer.json")
    )

  private val session = LocalSessionManager(sessionFile)
  private val terminal = Terminal(
    IO.blocking(readpassword.read("Enter password: "))
  )

  override def run(args: List[String]): IO[ExitCode] =
    CliArgs.pctl.parse(args) match {
      case Left(help) =>
        IO.println(help.toString)
          .as {
            if (help.errors.isEmpty) ExitCode.Success
            else ExitCode.Error
          }
      case Right(cmd) =>
        val cli = CommandLine(client, session, PlayBookRunner[IO], terminal)
        cli(cmd).as(ExitCode.Success)
    }

}
