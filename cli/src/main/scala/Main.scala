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
import org.http4s.Uri

import java.net.URI

object Main extends Platform {
  private val session = LocalSessionManager(".portainerrc")

  private def toUri(address: URI) =
    IO.fromEither(Uri.fromString(address.toString()))

  private def printResult[O: ResultPrinter](o: O) =
    IO.println(ResultPrinter[O].print(o))

  private def handle[O: ResultPrinter](
      server: ServerConfig,
      isPrint: Boolean,
      req: PortainerRequest[O]
  ): IO[Unit] =
    server match {
      case ServerConfig.Inline(address, token) =>
        if (isPrint)
          toUri(address)
            .map(PortainerClient.printer(_, PortainerCredential.Token(token)))
            .map(req.call)
            .map(_.toString())
            .flatMap(IO.println)
        else
          client.use(http =>
            toUri(address)
              .map(PortainerClient(_, http, PortainerCredential.Login(token)))
              .flatMap(req.call)
              .flatMap(printResult(_))
          )
      case ServerConfig.Session(name) =>
        for {
          s <- session.get(name)
          ses <- IO.fromOption(s)( // TODO better experience
            new Exception("Unknown server")
          )
          cred = PortainerCredential.Login(ses.token)
          address <- toUri(ses.address)
          _ <-
            if (isPrint)
              IO.println(req.call(PortainerClient.printer(address, cred)))
            else
              client
                .map(PortainerClient(address, _, cred))
                .use(req.call)
                .flatMap(printResult(_))
        } yield ()
    }

  import CLICommand._
  override def run(args: List[String]): IO[ExitCode] =
    CliArgs.pctl.parse(args) match {
      case Left(help) =>
        IO.println(help.toString)
          .as(if (help.errors.isEmpty) ExitCode.Success else ExitCode.Error)
      case Right(CLIOptions(a, isPrint)) =>
        (a match {
          case External(request, server) =>
            request match {
              case e: Requests.Stack.Get =>
                handle(server, isPrint, e)
              case e: Requests.Endpoint.Get =>
                handle(server, isPrint, e)
              case e: Requests.Stack.Listing =>
                handle(server, isPrint, e)
            }
          case Login(server, address, username, password) =>
            for {
              pass <- password.fold(readPassword)(IO(_))
              uri <- toUri(address)
              req = Requests.Login(username, pass)
              _ <-
                if (isPrint)
                  IO.println(req.call(PortainerClient.printer(uri)))
                else
                  client
                    .map(PortainerClient(uri, _))
                    .use(req.call)
                    .flatMap(token =>
                      session.add(server, Session(address, token.jwt))
                    ) >> IO.println("Logged in successfully!")
            } yield ()
          case Logout(server) =>
            session.load.flatMap(s =>
              session.save(s.copy(servers = s.servers - server))
            )
        }).as(ExitCode.Success)
    }

}
