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

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;

/** An event sourced entity handler */
public class EventSourcedEntityOneHandler
    extends EventSourcedEntityHandler<String, EventSourcedEntityOne> {

  public EventSourcedEntityOneHandler(EventSourcedEntityOne entity) {
    super(entity);
  }

  @Override
  public String handleEvent(String state, Object event) {
    if (event instanceof LocalPersistenceEventing.EventOne) {
      return entity().handle(state, (LocalPersistenceEventing.EventOne) event);
    } else if (event instanceof LocalPersistenceEventing.EventTwo) {
      return entity().handle(state, (LocalPersistenceEventing.EventTwo) event);
    } else {
      throw new EventSourcedEntityHandler.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, String state, Object command, CommandContext context) {
    switch (commandName) {
      case "EmitEvent":
        return entity().emitEvent(state, (LocalPersistenceEventing.EmitEventRequest) command);

      default:
        throw new EventSourcedEntityHandler.CommandHandlerNotFound(commandName);
    }
  }
}
