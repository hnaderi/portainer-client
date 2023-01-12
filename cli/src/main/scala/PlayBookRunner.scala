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
import dev.hnaderi.portainer.EndpointSelector.ById
import dev.hnaderi.portainer.EndpointSelector.ByName
import dev.hnaderi.portainer.EndpointSelector.ByTagIds
import dev.hnaderi.portainer.EndpointSelector.ByTags
import dev.hnaderi.portainer.Playbook.Deploy
import dev.hnaderi.portainer.Playbook.Destroy

object PlayBookRunner {

  private def getEndpointId[F[_]: Concurrent](
      client: PortainerClient[F]
  ): EndpointSelector => F[Int] = selector => {

    def assertSelected[T]: List[T] => F[T] = {
      case head :: Nil => head.pure[F]
      case selected =>
        CLIError
          .InvalidEndpointSelector(selector, selected.size)
          .raiseError[F, T]
    }

    selector match {
      case ById(value) => value.pure[F]
      case ByName(value) =>
        Requests.Endpoint
          .Listing(name = value.some)
          .call(client)
          .flatMap(assertSelected)
          .map(_.id)
      case ByTagIds(tagIds) =>
        Requests.Endpoint
          .Listing(tagIds = tagIds.toList)
          .call(client)
          .flatMap(assertSelected)
          .map(_.id)
      case ByTags(tags) =>
        Requests.Tag.Listing
          .call(client)
          .map(
            _.filter(t => tags.contains_(t.name))
              .map(_.endpoints)
              .reduce(_ intersect _)
              .toList
          )
          .flatMap(assertSelected)
    }
  }

  def apply[F[_]](implicit F: Async[F]): PlayBookRunnerBuilder[F] = client => {
    case Deploy(compose, selector, stack, env, inlineVars, configs, secrets) =>
      for {
        stackFile <- Utils.readLines(compose)

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

        _ <- client.use(client =>
          for {
            ep <- getEndpointId(client).apply(selector)
            swarm <- Requests.Swarm.Info(ep).call(client)

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
                  swarmId = swarm.swarmId,
                  endpointId = ep
                )
              case Some(stack) =>
                Requests.Stack.Update(
                  id = stack.id,
                  env = envVars,
                  compose = stackFile,
                  prune = true,
                  endpointId = ep
                )
            }

          } yield ()
        )
      } yield ()
    case Destroy(selector, stacks, configs, secrets) =>
      client.use(client =>
        for {
          endpoint <- getEndpointId(client).apply(selector)

          stacksL <- Requests.Stack
            .Listing(endpointId = Some(endpoint))
            .call(client)

          _ <- stacksL
            .filter(s => stacks.contains_(s.name))
            .map(s => Requests.Stack.Delete(s.id))
            .traverse(_.call(client))
          _ <- Requests.Config
            .Listing(endpoint, names = configs.map(_.toList).getOrElse(Nil))
            .call(client)
          _ <- Requests.Secret
            .Listing(endpoint, names = secrets.map(_.toList).getOrElse(Nil))
            .call(client)
        } yield ()
      )
  }

}
