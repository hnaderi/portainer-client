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

import io.circe.Decoder

sealed trait Models extends Serializable with Product
object Models {
  final case class Endpoint()
  object Endpoint {
    implicit val decoder: Decoder[Endpoint] = Decoder.const(Endpoint())

    final case class Get() extends PortainerRequest[Endpoint] with Models {
      override def call[F[_]](client: PortainerClient[F]): F[Endpoint] =
        client.get(_ / "endpoints" / "id")()
    }
    final case class Listing() extends PortainerRequest[List[Endpoint]] {
      override def call[F[_]](client: PortainerClient[F]): F[List[Endpoint]] =
        client.get(_ / "endpoints")()
    }

  }

  final case class EndpointGroup()
  object EndpointGroup {
    implicit val decoder: Decoder[EndpointGroup] =
      Decoder.const(EndpointGroup())

    final case class Get() extends PortainerRequest[EndpointGroup] {

      override def call[F[_]](client: PortainerClient[F]): F[EndpointGroup] =
        client.get(_ / "endpoints_groups" / "id")()

    }

  }

  final case class Registry()
  object Registry {
    implicit val decoder: Decoder[Registry] = Decoder.const(Registry())

    final case class Get() extends PortainerRequest[Registry] {

      override def call[F[_]](client: PortainerClient[F]): F[Registry] =
        client.get(_ / "registry")()

    }

  }

  final case class Stack()
  object Stack {
    implicit val decoder: Decoder[Stack] = Decoder.const(Stack())

    final case class Get() extends PortainerRequest[Stack] with Models {
      override def call[F[_]](client: PortainerClient[F]): F[Stack] =
        client.get(_ / "stacks" / "id")()
    }
    final case class Listing() extends PortainerRequest[List[Stack]] {

      override def call[F[_]](client: PortainerClient[F]): F[List[Stack]] =
        client.get(_ / "stacks")()

    }

  }
}
