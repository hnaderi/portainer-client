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

import cats.effect.kernel.Async
import cats.implicits._
import dev.hnaderi.portainer.Playbook.Deploy
import dev.hnaderi.portainer.Playbook.Destroy

object PlayBookRunner {
  def apply[F[_]](implicit F: Async[F]): PlayBookRunnerBuilder[F] = client => {
    case Deploy(compose, endpoint, stack, env, inlineVars, configs, secrets) =>
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

        _ <- client.use(client =>
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
    case Destroy(endpoint, stacks, configs, secrets) =>
      client.use(client =>
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
