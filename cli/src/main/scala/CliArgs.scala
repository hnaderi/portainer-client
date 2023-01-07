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
  private val stacks =
    Command("stack", "stack related actions")(
      Opts(CLICommand.External(Models.Stack.Get()))
    )
  private val endpoints =
    Command("endpoints", "stack related actions")(
      Opts(CLICommand.External(Models.Endpoint.Get()))
    )

  private val globalOptions = (
    Opts.flag("print", "just print curl and exit", "P").orFalse,
    Opts
      .option[String]("server", "use registered server name", "s")
      .map(ServerConfig.Session(_))
      .orElse(
        (
          Opts.option[URI]("address", "portainer http path to api", "H"),
          Opts.option[String]("token", "API token", "t")
        ).mapN(ServerConfig.Inline(_, _))
      )
  ).mapN(Config(_, _))

  val pctl: Command[(CLICommand, Config)] =
    Command("portainer", "portainer client")(
      Opts.subcommands(stacks, endpoints).product(globalOptions)
    )
}
