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
import java.nio.file.Path

object CliArgs {
  private def optionOrEnv[A: Argument](
      long: String,
      short: String,
      env: String,
      help: String
  ): Opts[A] =
    Opts.option[A](long, help, short).orElse(Opts.env(env, help))

  private val login =
    Command("login", "login to server and adds it to sessions")(
      (
        Opts.option[String]("name", "server name to add", "n"),
        optionOrEnv[URI](
          "address",
          "H",
          "PORTAINER_HOST",
          "portainer http path to api"
        ),
        optionOrEnv[String](
          "username",
          "u",
          "PORTAINER_USERNAME",
          "username to login to server"
        ),
        optionOrEnv[String](
          "password",
          "p",
          "PORTAINER_PASSWORD",
          "password to use for login"
        ).orNone
      ).mapN(CLICommand.Login(_, _, _, _))
    )

  private val logout =
    Command("logout", "removes server from logged in sessions")(
      Opts
        .argument[String]("session name")
        .map(CLICommand.Logout(_))
    )

  private val stacks: Command[PortainerRequest[?]] = {
    val listing =
      Command("list", "list all stacks")(Opts.unit.as(Requests.Stack.Listing()))
    val get = Command("get", "get stack by id")(
      Opts.argument[Int]("stack id").map(Requests.Stack.Get(_))
    )

    Command("stack", "stack related actions")(
      Opts.subcommands(listing, get)
    )
  }

  private val endpoints: Command[PortainerRequest[?]] = {
    val get = Command("get", "get endpoint by id")(
      Opts.argument[Int]("id").map(Requests.Endpoint.Get(_))
    )
    val listing = Command("list", "list endpoints")(
      (
        Opts
          .options[Int]("tag-id", "endpoints with tag id")
          .map(_.toList)
          .orNone
          .map(_.getOrElse(Nil)),
        Opts.option[String]("name", "endpoints with name").orNone
      ).mapN(Requests.Endpoint.Listing(_, _))
    )

    Command("endpoints", "endpoint related actions")(
      Opts.subcommands(get, listing)
    )
  }

  private val serverConfig: Opts[ServerConfig] = (Opts
    .option[String]("server", "use registered server name", "s", "session name")
    .map(ServerConfig.Session(_))
    .orElse(
      (
        Opts.option[URI]("address", "portainer http path to api", "H"),
        optionOrEnv[String]("token", "t", "PORTAINER_TOKEN", "API token")
      ).mapN(ServerConfig.Inline(_, _))
    ))

  private val endpointSelector = Opts
    .option[String]("endpoint", "endpoint name")
    .map(EndpointSelector.ByName(_))
    .orElse(
      Opts
        .option[Int]("endpoint-id", "endpoint id")
        .map(EndpointSelector.ById(_))
    )
    .orElse(
      Opts
        .options[String]("tag", "endpoint tag", "t")
        .map(EndpointSelector.ByTags(_))
    )
    .orElse(
      Opts
        .options[Int]("tag-id", "endpoint tag", "T")
        .map(EndpointSelector.ByTagIds(_))
    )

  private val confirm =
    Opts.flag("confirm", "Respond yes to all confirmations", "Y").orFalse

  private val deploy: Command[Playbook.Deploy] = {
    implicit val inlineEnvArg: Argument[InlineEnv] =
      Argument.from("key=value")(InlineEnv.validate)
    implicit val fileMappingArg: Argument[FileMapping] =
      Argument.from("path-to-file:resource-name")(FileMapping.validate)

    val immutableHelp =
      """NOTE that as configs and secrets in docker are immutable and might be already in use, your resource name must not exist!
Ensure using versioning or date in names to always get a new name.
"""

    Command("deploy", "deploys a stack and all its dependencies") {
      (
        Opts.option[Path]("compose-file", "compose file", "f"),
        endpointSelector,
        Opts.option[String]("stack", "stack name", "S"),
        Opts.option[Path]("env-file", "environment file", "E").orNone,
        Opts.options[InlineEnv]("env", "environment variable", "e").orNone,
        Opts
          .options[FileMapping]("config", s"config file; $immutableHelp")
          .orNone,
        Opts
          .options[FileMapping]("secret", s"secret file: $immutableHelp")
          .orNone,
        confirm
      ).mapN(Playbook.Deploy(_, _, _, _, _, _, _, _))
    }
  }

  private val destroy: Command[Playbook.Destroy] =
    Command("destroy", "destroy stacks, configs, secrets") {
      (
        endpointSelector,
        Opts.options[String]("stack", "stack name", "S"),
        Opts.options[String]("config", "config file").orNone,
        Opts.options[String]("secret", "secret file").orNone,
        confirm
      ).mapN(Playbook.Destroy(_, _, _, _, _))
    }

  private val playbooks = (serverConfig, Opts.subcommands(deploy, destroy))
    .mapN(CLICommand.Play(_, _))

  private val isPrint =
    Opts.flag("print", "just print curl and exit", "P").orFalse

  private val internal =
    Opts.subcommands(login, logout)

  private val external =
    (Opts.subcommands(stacks, endpoints), serverConfig, isPrint)
      .mapN(CLICommand.External(_, _, _))

  val pctl: Command[CLICommand] =
    Command(
      "portainer",
      """Portainer client
Save human time by using this client to automate workflows in CI/CD or other pipelines.
"""
    )(
      internal.orElse(external).orElse(playbooks)
    )
}
