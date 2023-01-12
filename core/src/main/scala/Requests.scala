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
import io.circe.Json
import io.circe.syntax._
import org.http4s.Method

import models._

object Requests {
  final case class Login(username: String, password: String)
      extends PortainerRequestBase[LoginToken] {
    override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
      client.send(
        _ / "auth",
        Method.POST,
        Map(
          "username" -> username,
          "password" -> password
        )
      )
  }

  object Endpoint {
    final case class Get(id: Int) extends PortainerRequestBase[Endpoint] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "endpoints" / id)
    }
    final case class Listing(
        tagIds: List[Int] = Nil,
        name: Option[String] = None
    ) extends PortainerRequestBase[List[Endpoint]] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(
          _ / "endpoints" ++? ("tagIds" -> tagIds) +?? ("name" -> name)
        )
    }
  }

  object Stack {
    final case class Get(
        id: Int
    ) extends PortainerRequestBase[Stack] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "stacks" / id)
    }
    final case class Listing(
        endpointId: Option[Int] = None,
        swarmId: Option[String] = None
    ) extends PortainerRequestBase[List[Stack]] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(
          _ / "stacks" +? ("filters" -> filterForM(
            "SwarmID" -> swarmId.asJson,
            "EndpointID" -> endpointId.asJson
          ))
        )
    }

    final case class Create(
        name: String,
        env: Map[String, String],
        compose: String,
        swarmId: String,
        endpointId: Int
    ) extends PortainerRequestBase[Stack] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(
          _ / "stacks" +?
            ("endpointId" -> endpointId) +?
            ("method" -> "string") +?
            ("type" -> "1"),
          Method.POST,
          StackCreateRequest(name, env, compose, swarmId)
        )
    }
    final case class Update(
        id: Int,
        env: Map[String, String],
        compose: String,
        prune: Boolean,
        endpointId: Int
    ) extends PortainerRequestBase[List[Stack]] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(
          _ / "stacks" / id +? ("endpointId" -> endpointId),
          Method.PUT,
          StackUpdateRequest(
            id,
            env,
            compose,
            prune
          )
        )
    }
    final case class Delete(id: Int) extends PortainerRequestRaw {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(_ / "stacks" / id, Method.DELETE)
    }

    final case class GetFile(id: String) extends PortainerRequestBase[Json] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "stacks" / id / "file")
    }

    final case class Start(id: String) extends PortainerRequestBase[Unit] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(_ / "stacks" / id / "start", Method.POST)
    }
    final case class Stop(id: String) extends PortainerRequestBase[Unit] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(_ / "stacks" / id / "stop", Method.POST)
    }
  }

  private def filterForM(kvs: (String, Json)*) = Json
    .obj(kvs: _*)
    .deepDropNullValues
    .noSpaces

  private def filterFor(id: Option[String], names: List[String]) = filterForM(
    "id" -> id.asJson,
    "names" -> names.toNel.asJson
  )

  object Config {
    final case class Create(
        endpoint: Int,
        name: String,
        labels: Map[String, String],
        data: String
    ) extends PortainerRequestRaw {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(
          _ / "endpoints" / endpoint / "docker" / "configs" / "create",
          Method.POST,
          ConfigCreateRequest(name, labels, data)
        )
    }

    final case class Delete(endpoint: Int, id: String)
        extends PortainerRequestRaw {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(
          _ / "endpoints" / endpoint / "docker" / "configs" / id,
          Method.DELETE
        )

    }

    final case class Listing(
        endpoint: Int,
        id: Option[String] = None,
        names: List[String] = Nil
    ) extends PortainerRequestBase[List[Config]] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(
          _ / "endpoints" / endpoint / "docker" / "configs" +? (
            "filters" -> filterFor(id, names)
          )
        )

    }
  }

  object Secret {
    final case class Create(
        endpoint: Int,
        name: String,
        labels: Map[String, String],
        data: String
    ) extends PortainerRequestRaw {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(
          _ / "endpoints" / endpoint / "docker" / "secrets" / "create",
          Method.POST,
          SecretCreateRequest(name, labels, data)
        )
    }

    final case class Delete(endpoint: Int, id: String)
        extends PortainerRequestRaw {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.send(
          _ / "endpoints" / endpoint / "docker" / "secrets" / id,
          Method.DELETE
        )

    }

    final case class Listing(
        endpoint: Int,
        id: Option[String] = None,
        names: List[String] = Nil
    ) extends PortainerRequestBase[List[Secret]] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(
          _ / "endpoints" / endpoint / "docker" / "secrets" +? (
            "filters" -> filterFor(id, names)
          )
        )

    }
  }

  object Tag {
    case object Listing extends PortainerRequestBase[List[Tag]] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "tags")
    }
  }

  object Swarm {
    final case class Info(endpoint: Int)
        extends PortainerRequestBase[SwarmInfo] {
      override def callRaw[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "endpoints" / endpoint / "docker" / "info")
    }
  }

}
