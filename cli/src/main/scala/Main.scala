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
import io.circe._
import org.http4s.Uri

import java.net.URI

object Main extends Platform {
  private val session = LocalSessionManager(".portainerrc")

  private def handle[O](config: Config, req: PortainerRequest[O])(
      run: O => IO[Unit]
  ): IO[Unit] =
    config.server match {
      case ServerConfig.Inline(address, token) =>
        if (config.print)
          IO.fromEither(Uri.fromString(address.toString()))
            .map(PortainerClient.printer(_, PortainerCredential.Token(token)))
            .map(req.call)
            .map(_.toString())
            .flatMap(IO.println)
        else
          client.use(http =>
            IO.fromEither(Uri.fromString(address.toString()))
              .map(PortainerClient(_, http, PortainerCredential.Login(token)))
              .flatMap(req.call)
              .flatMap(run)
          )
      case ServerConfig.Session(name) =>
        IO.print(name)
    }

  import CLICommand._
  override def run(args: List[String]): IO[ExitCode] =
    CliArgs.pctl.parse(args) match {
      case Left(help) =>
        IO.println(help.toString)
          .as(if (help.errors.isEmpty) ExitCode.Success else ExitCode.Error)
      case Right((a, config)) =>
        (a match {
          case External(model) =>
            model match {
              case e: Models.Stack.Get =>
                handle(config, e)(IO.println)
              case e: Models.Endpoint.Get =>
                handle(config, e)(IO.println)
            }
          case Login(server, username, password) =>
            password
              .fold(readPassword)(IO(_))
              .flatMap(pass => IO.println(s"""
server: $server
username: $username
password: $pass
"""))
          case Logout(server) =>
            session.load.flatMap(s =>
              session.save(s.copy(servers = s.servers - server))
            )
        }).as(ExitCode.Success)
    }

}

final case class Config(
    print: Boolean = false,
    server: ServerConfig
)

final case class Sessions(
    servers: Map[String, String]
)
object Sessions {
  import io.circe.syntax._
  implicit val encoder: Encoder[Sessions] =
    Encoder.instance(s => Json.obj("servers" -> s.servers.asJson))
  implicit val decoder: Decoder[Sessions] = (c: HCursor) =>
    c.downField("servers").as[Map[String, String]].map(Sessions(_))
}

sealed trait ServerConfig extends Serializable with Product
object ServerConfig {
  final case class Inline(address: URI, token: String) extends ServerConfig
  final case class Session(name: String) extends ServerConfig
}

sealed trait CLICommand extends Serializable with Product
object CLICommand {
  final case class External(model: Models) extends CLICommand
  final case class Login(
      server: String,
      username: String,
      password: Option[String]
  ) extends CLICommand
  final case class Logout(server: String) extends CLICommand
}
