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

import org.http4s.Method

import models._
import io.circe.Json

sealed trait Requests extends Serializable with Product
object Requests {
  final case class Login(username: String, password: String)
      extends PortainerRequest[LoginToken] {
    override def call[F[_]](client: PortainerClient[F]): F[LoginToken] =
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
    final case class Get() extends PortainerRequest[Endpoint] with Requests {
      override def call[F[_]](client: PortainerClient[F]): F[Endpoint] =
        client.get(_ / "endpoints" / "id")()
    }
    final case class Listing() extends PortainerRequest[List[Endpoint]] {
      override def call[F[_]](client: PortainerClient[F]): F[List[Endpoint]] =
        client.get(_ / "endpoints")()
    }

  }

  object EndpointGroup {
    final case class Get(id: String) extends PortainerRequest[Json] {
      override def call[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "endpoints_groups" / id)()
    }

  }

  object Registry {
    final case class Get() extends PortainerRequest[Registry] {

      override def call[F[_]](client: PortainerClient[F]): F[Registry] =
        client.get(_ / "registry")()

    }

  }

  object Stack {
    final case class Get(id: String)
        extends PortainerRequest[Json]
        with Requests {
      override def call[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "stacks" / id)()
    }
    final case class Listing() extends PortainerRequest[Json] with Requests {
      override def call[F[_]](client: PortainerClient[F]): F[Json] =
        client.get(_ / "stacks")()
    }

  }

}
