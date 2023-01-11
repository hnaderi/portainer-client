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
import io.circe.Json
import org.http4s.Uri
import org.http4s.client.Client

import java.net.URI

import CLICommand._

object CommandLine {
  def apply[F[_]](
      client: Resource[F, Client[F]],
      sessions: SessionManager[F],
      console: Terminal[F]
  )(implicit F: Concurrent[F]): CommandLine[F] = { cmd =>
    def toUri(address: URI) =
      F.fromEither(Uri.fromString(address.toString()))

    def printResult(o: Json) =
      console.println(o.spaces2)

    def handle(
        server: ServerConfig,
        isPrint: Boolean,
        req: PortainerRequest[?]
    ): F[Unit] =
      server match {
        case ServerConfig.Inline(address, token) =>
          if (isPrint)
            toUri(address)
              .map(PortainerClient.printer(_, PortainerCredential.Token(token)))
              .map(req.callRaw)
              .flatMap(console.println(_))
          else
            client.use(http =>
              toUri(address)
                .map(PortainerClient(_, http, PortainerCredential.Login(token)))
                .flatMap(req.callRaw)
                .flatMap(printResult(_))
            )
        case ServerConfig.Session(name) =>
          for {
            s <- sessions.get(name)
            ses <- F.fromOption(
              s, // TODO better experience
              new Exception("Unknown server")
            )
            cred = PortainerCredential.Login(ses.token)
            address <- toUri(ses.address)
            _ <-
              if (isPrint)
                console.println(
                  req.callRaw(PortainerClient.printer(address, cred))
                )
              else
                client
                  .map(PortainerClient(address, _, cred))
                  .use(req.callRaw)
                  .flatMap(printResult(_))
          } yield ()
      }

    cmd match {
      case External(request, server, print) => handle(server, print, request)
      case Logout(server) =>
        sessions.load.flatMap(s =>
          sessions.save(s.copy(servers = s.servers - server))
        )
      case Login(server, address, username, password, print) =>
        for {
          pass <- password.fold(console.readPassword)(F.pure(_))
          uri <- toUri(address)
          req = Requests.Login(username, pass)
          _ <-
            if (print)
              console.println(
                req.callRaw(PortainerClient.printer(uri)).toString()
              )
            else
              client
                .map(PortainerClient(uri, _))
                .use(req.call(_))
                .flatMap(token =>
                  sessions.add(server, Session(address, token.jwt))
                ) >> console.println("Logged in successfully!")
        } yield ()

      // case Deploy(
      //       compose,
      //       endpoint,
      //       stack,
      //       env,
      //       inlineVars,
      //       configs,
      //       secrets
      //     ) =>
      //   ???
      // case Destroy(endpoint, stacks, configs, secrets) => ???
      case _ => ???
    }

  }

}
