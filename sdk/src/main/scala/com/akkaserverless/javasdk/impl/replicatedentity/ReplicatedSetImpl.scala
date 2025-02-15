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

import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.replicatedentity.ReplicatedSet
import com.akkaserverless.protocol.replicated_entity.{ReplicatedEntityDelta, ReplicatedSetDelta}

import scala.jdk.CollectionConverters._

private[replicatedentity] class ReplicatedSetImpl[E](
    anySupport: AnySupport,
    value: Set[E] = Set.empty[E],
    added: Set[E] = Set.empty[E],
    removed: Set[E] = Set.empty[E],
    cleared: Boolean = false
) extends ReplicatedSet[E]
    with InternalReplicatedData {

  override type Self = ReplicatedSetImpl[E]
  override val name = "ReplicatedSet"

  override def size: Int = value.size

  override def isEmpty: Boolean = value.isEmpty

  override def elements: java.util.Set[E] = value.asJava

  override def iterator(): java.util.Iterator[E] = value.iterator.asJava

  override def contains(element: E): Boolean = value.contains(element)

  override def add(element: E): ReplicatedSetImpl[E] =
    if (value.contains(element)) {
      this
    } else {
      if (removed.contains(element)) {
        new ReplicatedSetImpl(anySupport, value + element, added, removed - element, cleared)
      } else {
        new ReplicatedSetImpl(anySupport, value + element, added + element, removed, cleared)
      }
    }

  override def remove(element: E): ReplicatedSetImpl[E] =
    if (!value.contains(element)) {
      this
    } else {
      if (value.size == 1) { // just the to-be-removed element
        clear()
      } else {
        if (added.contains(element)) {
          new ReplicatedSetImpl(anySupport, value - element, added - element, removed, cleared)
        } else {
          new ReplicatedSetImpl(anySupport, value - element, added, removed + element, cleared)
        }
      }
    }

  override def containsAll(elements: java.util.Collection[E]): Boolean = elements.asScala.forall(value.contains)

  override def addAll(elements: java.util.Collection[E]): ReplicatedSetImpl[E] =
    elements.asScala.foldLeft(this) { case (set, element) => set.add(element) }

  override def retainAll(elements: java.util.Collection[E]): ReplicatedSetImpl[E] =
    value.foldLeft(this) { case (set, element) => if (!elements.contains(element)) set.remove(element) else set }

  override def removeAll(elements: java.util.Collection[E]): ReplicatedSetImpl[E] =
    elements.asScala.foldLeft(this) { case (set, element) => set.remove(element) }

  override def clear(): ReplicatedSetImpl[E] =
    new ReplicatedSetImpl[E](anySupport, cleared = true)

  override def hasDelta: Boolean = cleared || added.nonEmpty || removed.nonEmpty

  override def getDelta: ReplicatedEntityDelta.Delta =
    ReplicatedEntityDelta.Delta.ReplicatedSet(
      ReplicatedSetDelta(
        cleared,
        removed = removed.map(anySupport.encodeScala).toSeq,
        added = added.map(anySupport.encodeScala).toSeq
      )
    )

  override def resetDelta(): ReplicatedSetImpl[E] =
    if (hasDelta) new ReplicatedSetImpl(anySupport, value) else this

  override val applyDelta: PartialFunction[ReplicatedEntityDelta.Delta, ReplicatedSetImpl[E]] = {
    case ReplicatedEntityDelta.Delta.ReplicatedSet(ReplicatedSetDelta(cleared, removed, added, _)) =>
      val updatedValue = {
        (if (cleared) Set.empty[E] else value -- removed.map(element => anySupport.decode(element).asInstanceOf[E])) ++
        added.map(element => anySupport.decode(element).asInstanceOf[E])
      }
      new ReplicatedSetImpl(anySupport, updatedValue)
  }

  override def toString = s"ReplicatedSet(${value.mkString(",")})"
}
