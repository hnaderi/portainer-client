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
import dev.hnaderi.portainer.ServerConfig.Inline
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
  )(implicit F: Async[F]): CommandLine[F] = { cmd =>
    def toUri(address: URI) =
      F.fromEither(Uri.fromString(address.toString()))

    def printResult(o: Json) =
      console.println(o.spaces2)

    def getClient(server: ServerConfig): Resource[F, PortainerClient[F]] =
      client.evalMap(http =>
        server match {
          case Inline(address, token) =>
            toUri(address).map(
              PortainerClient(_, http, PortainerCredential.Token(token))
            )
          case ServerConfig.Session(name) =>
            sessions.get(name).flatMap {
              case Some(Session(address, token)) =>
                toUri(address).map(
                  PortainerClient(_, http, PortainerCredential.Login(token))
                )
              case None =>
                // TODO better experience
                F.raiseError[PortainerClient[F]](
                  new Exception("Unknown server")
                )
            }
        }
      )

    def getPrinter(server: ServerConfig): F[PortainerClient[Printed[F, *]]] =
      server match {
        case Inline(address, token) =>
          toUri(address).map(
            PortainerClient.printer(_, PortainerCredential.Token(token))
          )
        case ServerConfig.Session(name) =>
          sessions.get(name).flatMap {
            case Some(Session(address, token)) =>
              toUri(address).map(
                PortainerClient.printer(_, PortainerCredential.Login(token))
              )
            case None =>
              // TODO better experience
              F.raiseError(
                new Exception("Unknown server")
              )
          }
      }

    def handle(
        server: ServerConfig,
        isPrint: Boolean,
        req: PortainerRequest[?]
    ): F[Unit] =
      if (isPrint)
        getPrinter(server).map(req.callRaw).flatMap(console.println(_))
      else getClient(server).use(req.callRaw).flatMap(printResult)

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

      case Deploy(
            server,
            compose,
            endpoint,
            stack,
            env,
            inlineVars,
            configs,
            secrets
          ) =>
        for {
          stackFile <- Utils.readLines(compose)

          ep = endpoint.head // TODO implement tag search

          envVars <- env
            .fold(F.pure(Map.empty[String, String]))(Utils.readEnvFile[F])
            .map(
              _ ++ inlineVars
                .map(_.toList)
                .getOrElse(Nil)
                .map(v => (v.key, v.value))
            )
          configMaps <- configs.fold(F.pure(Map.empty[String, String]))(
            Utils.readFileMaps[F]
          )
          secretMaps <- secrets.fold(F.pure(Map.empty[String, String]))(
            Utils.readFileMaps[F]
          )

          _ <- getClient(server).use(client =>
            for {
              _ <- configMaps.toList.traverse { case (name, content) =>
                Requests.Config
                  .Create(ep, name, Map.empty, content)
                  .call(client)
              }
              _ <- secretMaps.toList.traverse { case (name, content) =>
                Requests.Secret
                  .Create(ep, name, Map.empty, content)
                  .call(client)
              }

              stacks <- Requests.Stack.Listing().call(client)

              _ = stacks.find(_.name == stack) match {
                case None =>
                  Requests.Stack.Create(
                    name = stack,
                    env = envVars,
                    compose = stackFile,
                    swarmId = "",
                    endpointId = ""
                  )
                case Some(stack) =>
                  Requests.Stack.Update(
                    id = stack.id,
                    env = envVars,
                    compose = stackFile,
                    prune = true,
                    endpointId = ""
                  )
              }

            } yield ()
          )
        } yield ()

      case Destroy(server, endpoint, stacks, configs, secrets) =>
        getClient(server).use(client =>
          for {
            stacksL <- Requests.Stack.Listing().call(client)

            ep = endpoint.head // TODO implement tag search

            _ <- stacksL
              .filter(s => stacks.contains_(s.name))
              .map(s => Requests.Stack.Delete(s.id))
              .traverse(_.call(client))
            _ <- Requests.Config
              .Listing(ep, names = configs.map(_.toList).getOrElse(Nil))
              .call(client)
            _ <- Requests.Secret
              .Listing(ep, names = secrets.map(_.toList).getOrElse(Nil))
              .call(client)
          } yield ()
        )
    }

  }
}
