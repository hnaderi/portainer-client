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

import cats.implicits._
import com.monovore.decline._

import java.net.URI

object CliArgs {
  private val login =
    Command("login", "login to server and adds it to sessions")(
      (
        Opts.option[String]("name", "server name to add", "n"),
        Opts.option[URI]("address", "portainer http path to api", "H"),
        Opts.option[String]("username", "username to login to server", "u"),
        Opts.option[String]("password", "password to use for login", "p").orNone
      ).mapN(CLICommand.Login(_, _, _, _))
    )

  private val logout =
    Command("logout", "removes server from logged in sessions")(
      Opts
        .argument[String]("session name")
        .map(CLICommand.Logout(_))
    )

  private val stacks: Command[Requests] = {
    val listing =
      Command("list", "list all stacks")(Opts.unit.as(Requests.Stack.Listing()))
    val get = Command("get", "get stack by id")(
      Opts.argument[String]("stack id").map(Requests.Stack.Get(_))
    )

    Command("stack", "stack related actions")(
      Opts.subcommands(listing, get)
    )
  }

  private val endpoints: Command[Requests] =
    Command("endpoints", "endpoint related actions")(
      Opts(Requests.Endpoint.Get())
    )

  private val isPrint =
    Opts.flag("print", "just print curl and exit", "P").orFalse
  private val serverConfig: Opts[ServerConfig] = (Opts
    .option[String]("server", "use registered server name", "s")
    .map(ServerConfig.Session(_))
    .orElse(
      (
        Opts.option[URI]("address", "portainer http path to api", "H"),
        Opts.option[String]("token", "API token", "t")
      ).mapN(ServerConfig.Inline(_, _))
    ))

  private val internal =
    Opts.subcommands(login, logout)

  private val external =
    (Opts.subcommands(stacks, endpoints), serverConfig)
      .mapN(CLICommand.External(_, _))

  val pctl: Command[CLIOptions] =
    Command("portainer", "portainer client")(
      (internal.orElse(external), isPrint).mapN(CLIOptions(_, _))
    )
}
