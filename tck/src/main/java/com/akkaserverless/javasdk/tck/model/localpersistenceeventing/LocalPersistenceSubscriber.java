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

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.JsonSupport;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.tck.model.eventing.LocalPersistenceSubscriberModel;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Any;

public class LocalPersistenceSubscriber extends Action {

  public LocalPersistenceSubscriber(ActionCreationContext creationContext) {}

  public Action.Effect<LocalPersistenceEventing.Response> processEventOne(
      LocalPersistenceEventing.EventOne eventOne) {
    return convert(actionContext(), eventOne.getStep());
  }

  public Source<Action.Effect<LocalPersistenceEventing.Response>, NotUsed> processEventTwo(
      LocalPersistenceEventing.EventTwo eventTwo) {
    ActionContext context = actionContext();
    return Source.from(eventTwo.getStepList()).map(step -> convert(context, step));
  }

  public Action.Effect<LocalPersistenceEventing.Response> processAnyEvent(Any any) {
    JsonMessage jsonMessage = JsonSupport.decodeJson(JsonMessage.class, any);
    return effects()
        .reply(
            LocalPersistenceEventing.Response.newBuilder()
                .setId(actionContext().eventSubject().orElse(""))
                .setMessage(jsonMessage.message)
                .build());
  }

  public Action.Effect<LocalPersistenceEventing.Response> processValueOne(
      LocalPersistenceEventing.ValueOne valueOne) {
    ActionContext context = actionContext();
    return convert(context, valueOne.getStep());
  }

  public Source<Action.Effect<LocalPersistenceEventing.Response>, NotUsed> processValueTwo(
      LocalPersistenceEventing.ValueTwo valueTwo) {
    ActionContext context = actionContext();
    return Source.from(valueTwo.getStepList()).map(step -> convert(context, step));
  }

  public Effect<LocalPersistenceEventing.Response> processAnyValue(Any any) {
    JsonMessage jsonMessage = JsonSupport.decodeJson(JsonMessage.class, any);
    return effects()
        .reply(
            LocalPersistenceEventing.Response.newBuilder()
                .setId(actionContext().eventSubject().orElse(""))
                .setMessage(jsonMessage.message)
                .build());
  }

  public Effect<LocalPersistenceEventing.Response> effect(
      LocalPersistenceEventing.EffectRequest request) {
    return effects()
        .reply(
            LocalPersistenceEventing.Response.newBuilder()
                .setId(request.getId())
                .setMessage(request.getMessage())
                .build());
  }

  private Action.Effect<LocalPersistenceEventing.Response> convert(
      ActionContext context, LocalPersistenceEventing.ProcessStep step) {
    String id = context.eventSubject().orElse("");
    if (step.hasReply()) {
      return effects()
          .reply(
              LocalPersistenceEventing.Response.newBuilder()
                  .setId(id)
                  .setMessage(step.getReply().getMessage())
                  .build());
    } else if (step.hasForward()) {
      return effects()
          .forward(
              context
                  .serviceCallFactory()
                  .lookup(
                      LocalPersistenceSubscriberModel.name,
                      "Effect",
                      LocalPersistenceEventing.EffectRequest.class)
                  .createCall(
                      LocalPersistenceEventing.EffectRequest.newBuilder()
                          .setId(id)
                          .setMessage(step.getForward().getMessage())
                          .build()));
    } else {
      throw new RuntimeException("No reply or forward");
    }
  }
}
