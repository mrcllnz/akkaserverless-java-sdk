/*
 * Copyright 2021 Lightbend Inc.
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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity._
import com.akkaserverless.javasdk.impl.AnySupport

final class ReplicatedDataFactoryImpl(anySupport: AnySupport) extends ReplicatedDataFactory {
  private var _internalData: InternalReplicatedData = _

  def internalData: InternalReplicatedData = _internalData

  private def newData[D <: InternalReplicatedData](data: D): D = {
    if (_internalData ne null)
      throw new IllegalStateException("A ReplicatedDataFactory must only be used to create one replicated data object")
    _internalData = data
    data
  }

  override def newCounter(): ReplicatedCounter =
    newData(new ReplicatedCounterImpl)

  override def newReplicatedCounterMap[K](): ReplicatedCounterMap[K] =
    newData(new ReplicatedCounterMapImpl[K](anySupport))

  override def newReplicatedSet[T](): ReplicatedSet[T] =
    newData(new ReplicatedSetImpl[T](anySupport))

  override def newRegister[T](value: T): ReplicatedRegister[T] =
    newData(new ReplicatedRegisterImpl[T](anySupport, value, Option(value).map(anySupport.encodeScala)))

  override def newReplicatedRegisterMap[K, V](): ReplicatedRegisterMap[K, V] =
    newData(new ReplicatedRegisterMapImpl[K, V](anySupport))

  override def newReplicatedMultiMap[K, V](): ReplicatedMultiMap[K, V] =
    newData(new ReplicatedMultiMapImpl[K, V](anySupport))

  override def newReplicatedMap[K, V <: ReplicatedData](): ReplicatedMap[K, V] =
    newData(new ReplicatedMapImpl[K, InternalReplicatedData](anySupport)).asInstanceOf[ReplicatedMap[K, V]]

  override def newVote(): ReplicatedVote =
    newData(new ReplicatedVoteImpl)
}
