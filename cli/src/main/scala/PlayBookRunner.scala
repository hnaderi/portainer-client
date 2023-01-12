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

import cats.Monad
import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import dev.hnaderi.portainer.EndpointSelector.ById
import dev.hnaderi.portainer.EndpointSelector.ByName
import dev.hnaderi.portainer.EndpointSelector.ByTagIds
import dev.hnaderi.portainer.EndpointSelector.ByTags
import dev.hnaderi.portainer.Playbook.Deploy
import dev.hnaderi.portainer.Playbook.Destroy

import java.nio.file.Path

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

  private def buildEnvMap[F[_]](
      env: Option[Path],
      inlineVars: Option[NonEmptyList[InlineEnv]]
  )(implicit F: Async[F]) = env
    .fold(F.pure(Map.empty[String, String]))(Utils.readEnvFile[F])
    .map(
      _ ++ inlineVars
        .map(_.toList)
        .getOrElse(Nil)
        .map(v => (v.key, v.value))
    )

  private def buildDataMaps[F[_]](mappings: Option[NonEmptyList[FileMapping]])(
      implicit F: Async[F]
  ) =
    mappings.fold(F.pure(Map.empty[String, String]))(Utils.readFileMaps[F])

  private def confirm[F[_]](
      byPass: Boolean
  )(details: Seq[String])(implicit term: Terminal[F], F: Monad[F]) =
    if (byPass) byPass.pure[F]
    else term.confirm(s"${details.mkString("\n")}\n\nDo you want to proceed?")

  def apply[F[_]: Async: Terminal]: PlayBookRunnerBuilder[F] = client => {
    case Deploy(
          compose,
          selector,
          stack,
          env,
          inlineVars,
          configs,
          secrets,
          confirmed
        ) =>
      for {
        stackFile <- Utils.readLines(compose)
        envVars <- buildEnvMap(env, inlineVars)
        configMaps <- buildDataMaps(configs)
        secretMaps <- buildDataMaps(secrets)

        _ <- client.use(client =>
          for {
            ep <- getEndpointId(client).apply(selector)
            swarm <- Requests.Swarm.Info(ep).call(client)
            stacks <- Requests.Stack.Listing(endpointId = Some(ep)).call(client)

            (applyStack, isUpdate) = stacks.find(_.name == stack) match {
              case None =>
                Requests.Stack.Create(
                  name = stack,
                  env = envVars,
                  compose = stackFile,
                  swarmId = swarm.swarmId,
                  endpointId = ep
                ) -> false
              case Some(stack) =>
                Requests.Stack.Update(
                  id = stack.id,
                  env = envVars,
                  compose = stackFile,
                  prune = true,
                  endpointId = ep
                ) -> true
            }

            proceed <- confirm(confirmed) {
              s"stack $stack will be ${if (isUpdate) "updated" else "created"}" ::
                configMaps.keys
                  .map(c => s"config $c will be created")
                  .toList :::
                secretMaps.keys.map(s => s"secret $s will be created").toList
            }

            _ <-
              if (proceed)
                Terminal[F].println(s"applying stack ${stack}") >>
                  applyStack.call(client) >>
                  configMaps.toList.traverse { case (name, content) =>
                    Terminal[F].println(s"creating config ${name}") >>
                      Requests.Config
                        .Create(ep, name, Map.empty, content)
                        .call(client)
                  } >>
                  secretMaps.toList.traverse { case (name, content) =>
                    Terminal[F].println(s"creating secret ${name}") >>
                      Requests.Secret
                        .Create(ep, name, Map.empty, content)
                        .call(client)
                  }.void
              else Terminal[F].println("Discarded!")

          } yield ()
        )
      } yield ()
    case Destroy(selector, stacks, configs, secrets, confirmed) =>
      client.use(client =>
        for {
          endpoint <- getEndpointId(client).apply(selector)

          stacksL <- Requests.Stack
            .Listing(endpointId = Some(endpoint))
            .call(client)

          stacksToDelete = stacksL
            .filter(s => stacks.contains_(s.name))
          configsToDelete <- Requests.Config
            .Listing(endpoint, names = configs.map(_.toList).getOrElse(Nil))
            .call(client)
          secretsToDelete <- Requests.Secret
            .Listing(endpoint, names = secrets.map(_.toList).getOrElse(Nil))
            .call(client)

          proceed <- confirm(confirmed)(
            stacksToDelete.map(s =>
              s"stack [name: ${s.name}, id: ${s.id}] will be deleted"
            ) :::
              configsToDelete.map(c =>
                s"config [name: ${c.name}, id: ${c.id}] will be deleted"
              ) :::
              secretsToDelete.map(s =>
                s"secret [name: ${s.name}, id: ${s.id}] will be deleted"
              )
          )

          _ <-
            if (proceed)
              stacksToDelete.traverse(s =>
                Terminal[F].println(s"deleting stack ${s.id} ...") >>
                  Requests.Stack.Delete(s.id).call(client)
              ) >>
                configsToDelete.traverse(c =>
                  Terminal[F].println(s"deleting config ${c.id} ...") >>
                    Requests.Config.Delete(endpoint, c.id).call(client)
                ) >>
                secretsToDelete
                  .traverse(s =>
                    Terminal[F].println(s"deleting secret ${s.id} ...") >>
                      Requests.Secret.Delete(endpoint, s.id).call(client)
                  )
                  .void
            else Terminal[F].println("Discarded!")

        } yield ()
      )
  }

}
