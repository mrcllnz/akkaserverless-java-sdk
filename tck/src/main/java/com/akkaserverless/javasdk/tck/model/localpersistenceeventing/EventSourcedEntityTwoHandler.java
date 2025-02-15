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

import com.akkaserverless.javasdk.JsonSupport;
import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Any;

/** An event sourced entity handler */
public class EventSourcedEntityTwoHandler
    extends EventSourcedEntityHandler<String, EventSourcedEntityTwo> {

  public EventSourcedEntityTwoHandler(EventSourcedEntityTwo entity) {
    super(entity);
  }

  @Override
  public String handleEvent(String state, Object event) {
    // FIXME requirement to use JSON events should be removed from TCK
    if (event instanceof Any) {
      return entity().handle(state, JsonSupport.decodeJson(JsonMessage.class, (Any) event));
    } else {
      throw new EventSourcedEntityHandler.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, String state, Object command, CommandContext context) {
    switch (commandName) {
      case "EmitJsonEvent":
        return entity().emitJsonEvent(state, (LocalPersistenceEventing.JsonEvent) command);

      default:
        throw new EventSourcedEntityHandler.CommandHandlerNotFound(commandName);
    }
  }
}
