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

object Main extends Platform {
  private val session = LocalSessionManager(".portainerrc")
  private val terminal = Terminal(
    IO.blocking(readpassword.read("Enter password: "))
  )

  import CLICommand._
  override def run(args: List[String]): IO[ExitCode] =
    CliArgs.pctl.parse(args) match {
      case Left(help) =>
        IO.println(help.toString)
          .as {
            if (help.errors.isEmpty) ExitCode.Success
            else ExitCode.Error
          }
      case Right(cmd) =>
        val cli = CommandLineImpl(client, session, terminal)
        cmd match {
          case External(request, server, isPrint) =>
            cli.raw(server, request, isPrint).as(ExitCode.Success)
          case Login(server, address, username, password, isPrint) =>
            cli
              .login(server, address, username, password, isPrint)
              .as(ExitCode.Success)
          case Logout(server) => cli.logout(server).as(ExitCode.Success)
          case _ =>
            terminal.error("Not implemented yet!").as(ExitCode.Error)
        }
    }

}
