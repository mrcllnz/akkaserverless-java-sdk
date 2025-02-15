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

// Generated by Akka gRPC. DO NOT EDIT.
package com.example;

import akka.actor.ClassicActorSystemProvider;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;

import akka.grpc.internal.*;
import akka.grpc.GrpcClientSettings;
import akka.grpc.javadsl.AkkaGrpcClient;

import io.grpc.MethodDescriptor;

import static com.example.CounterService.Serializers.*;

import scala.concurrent.ExecutionContext;


import akka.grpc.javadsl.SingleResponseRequestBuilder;


public abstract class CounterServiceClient extends CounterServiceClientPowerApi implements CounterService, AkkaGrpcClient {
  public static final CounterServiceClient create(GrpcClientSettings settings, ClassicActorSystemProvider sys) {
    return new DefaultCounterServiceClient(settings, sys);
  }

  protected final static class DefaultCounterServiceClient extends CounterServiceClient {

      private final ClientState clientState;
      private final GrpcClientSettings settings;
      private final io.grpc.CallOptions options;
      private final Materializer mat;
      private final ExecutionContext ec;

      private DefaultCounterServiceClient(GrpcClientSettings settings, ClassicActorSystemProvider sys) {
        this.settings = settings;
        this.mat = SystemMaterializer.get(sys).materializer();
        this.ec = sys.classicSystem().dispatcher();
        this.clientState = new ClientState(
          settings,
          akka.event.Logging$.MODULE$.apply(sys.classicSystem(), DefaultCounterServiceClient.class, akka.event.LogSource$.MODULE$.<DefaultCounterServiceClient>fromAnyClass()),
          sys);
        this.options = NettyClientUtils.callOptions(settings);

        sys.classicSystem().getWhenTerminated().whenComplete((v, e) -> close());
      }

  
    
      private final SingleResponseRequestBuilder<com.example.CounterApi.IncreaseValue, com.google.protobuf.Empty> increaseRequestBuilder(akka.grpc.internal.InternalChannel channel){
        return new JavaUnaryRequestBuilder<>(increaseDescriptor, channel, options, settings, ec);
      }
    
  
    
      private final SingleResponseRequestBuilder<com.example.CounterApi.DecreaseValue, com.google.protobuf.Empty> decreaseRequestBuilder(akka.grpc.internal.InternalChannel channel){
        return new JavaUnaryRequestBuilder<>(decreaseDescriptor, channel, options, settings, ec);
      }
    
  
    
      private final SingleResponseRequestBuilder<com.example.CounterApi.ResetValue, com.google.protobuf.Empty> resetRequestBuilder(akka.grpc.internal.InternalChannel channel){
        return new JavaUnaryRequestBuilder<>(resetDescriptor, channel, options, settings, ec);
      }
    
  
    
      private final SingleResponseRequestBuilder<com.example.CounterApi.GetCounter, com.example.CounterApi.CurrentCounter> getCurrentCounterRequestBuilder(akka.grpc.internal.InternalChannel channel){
        return new JavaUnaryRequestBuilder<>(getCurrentCounterDescriptor, channel, options, settings, ec);
      }
    
  

      

        /**
         * For access to method metadata use the parameterless version of increase
         */
        public java.util.concurrent.CompletionStage<com.google.protobuf.Empty> increase(com.example.CounterApi.IncreaseValue request) {
          return increase().invoke(request);
        }

        /**
         * Lower level "lifted" version of the method, giving access to request metadata etc.
         * prefer increase(com.example.CounterApi.IncreaseValue) if possible.
         */
        
          public SingleResponseRequestBuilder<com.example.CounterApi.IncreaseValue, com.google.protobuf.Empty> increase()
        
        {
          return increaseRequestBuilder(clientState.internalChannel());
        }
      

        /**
         * For access to method metadata use the parameterless version of decrease
         */
        public java.util.concurrent.CompletionStage<com.google.protobuf.Empty> decrease(com.example.CounterApi.DecreaseValue request) {
          return decrease().invoke(request);
        }

        /**
         * Lower level "lifted" version of the method, giving access to request metadata etc.
         * prefer decrease(com.example.CounterApi.DecreaseValue) if possible.
         */
        
          public SingleResponseRequestBuilder<com.example.CounterApi.DecreaseValue, com.google.protobuf.Empty> decrease()
        
        {
          return decreaseRequestBuilder(clientState.internalChannel());
        }
      

        /**
         * For access to method metadata use the parameterless version of reset
         */
        public java.util.concurrent.CompletionStage<com.google.protobuf.Empty> reset(com.example.CounterApi.ResetValue request) {
          return reset().invoke(request);
        }

        /**
         * Lower level "lifted" version of the method, giving access to request metadata etc.
         * prefer reset(com.example.CounterApi.ResetValue) if possible.
         */
        
          public SingleResponseRequestBuilder<com.example.CounterApi.ResetValue, com.google.protobuf.Empty> reset()
        
        {
          return resetRequestBuilder(clientState.internalChannel());
        }
      

        /**
         * For access to method metadata use the parameterless version of getCurrentCounter
         */
        public java.util.concurrent.CompletionStage<com.example.CounterApi.CurrentCounter> getCurrentCounter(com.example.CounterApi.GetCounter request) {
          return getCurrentCounter().invoke(request);
        }

        /**
         * Lower level "lifted" version of the method, giving access to request metadata etc.
         * prefer getCurrentCounter(com.example.CounterApi.GetCounter) if possible.
         */
        
          public SingleResponseRequestBuilder<com.example.CounterApi.GetCounter, com.example.CounterApi.CurrentCounter> getCurrentCounter()
        
        {
          return getCurrentCounterRequestBuilder(clientState.internalChannel());
        }
      

      
        private static MethodDescriptor<com.example.CounterApi.IncreaseValue, com.google.protobuf.Empty> increaseDescriptor =
          MethodDescriptor.<com.example.CounterApi.IncreaseValue, com.google.protobuf.Empty>newBuilder()
            .setType(
   MethodDescriptor.MethodType.UNARY 
  
  
  
)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("com.example.CounterService", "Increase"))
            .setRequestMarshaller(new ProtoMarshaller<com.example.CounterApi.IncreaseValue>(IncreaseValueSerializer))
            .setResponseMarshaller(new ProtoMarshaller<com.google.protobuf.Empty>(EmptySerializer))
            .setSampledToLocalTracing(true)
            .build();
        
        private static MethodDescriptor<com.example.CounterApi.DecreaseValue, com.google.protobuf.Empty> decreaseDescriptor =
          MethodDescriptor.<com.example.CounterApi.DecreaseValue, com.google.protobuf.Empty>newBuilder()
            .setType(
   MethodDescriptor.MethodType.UNARY 
  
  
  
)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("com.example.CounterService", "Decrease"))
            .setRequestMarshaller(new ProtoMarshaller<com.example.CounterApi.DecreaseValue>(DecreaseValueSerializer))
            .setResponseMarshaller(new ProtoMarshaller<com.google.protobuf.Empty>(EmptySerializer))
            .setSampledToLocalTracing(true)
            .build();
        
        private static MethodDescriptor<com.example.CounterApi.ResetValue, com.google.protobuf.Empty> resetDescriptor =
          MethodDescriptor.<com.example.CounterApi.ResetValue, com.google.protobuf.Empty>newBuilder()
            .setType(
   MethodDescriptor.MethodType.UNARY 
  
  
  
)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("com.example.CounterService", "Reset"))
            .setRequestMarshaller(new ProtoMarshaller<com.example.CounterApi.ResetValue>(ResetValueSerializer))
            .setResponseMarshaller(new ProtoMarshaller<com.google.protobuf.Empty>(EmptySerializer))
            .setSampledToLocalTracing(true)
            .build();
        
        private static MethodDescriptor<com.example.CounterApi.GetCounter, com.example.CounterApi.CurrentCounter> getCurrentCounterDescriptor =
          MethodDescriptor.<com.example.CounterApi.GetCounter, com.example.CounterApi.CurrentCounter>newBuilder()
            .setType(
   MethodDescriptor.MethodType.UNARY 
  
  
  
)
            .setFullMethodName(MethodDescriptor.generateFullMethodName("com.example.CounterService", "GetCurrentCounter"))
            .setRequestMarshaller(new ProtoMarshaller<com.example.CounterApi.GetCounter>(GetCounterSerializer))
            .setResponseMarshaller(new ProtoMarshaller<com.example.CounterApi.CurrentCounter>(CurrentCounterSerializer))
            .setSampledToLocalTracing(true)
            .build();
        

      /**
       * Initiates a shutdown in which preexisting and new calls are cancelled.
       */
      public java.util.concurrent.CompletionStage<akka.Done> close() {
        return clientState.closeCS() ;
      }

     /**
      * Returns a CompletionState that completes successfully when shutdown via close()
      * or exceptionally if a connection can not be established after maxConnectionAttempts.
      */
      public java.util.concurrent.CompletionStage<akka.Done> closed() {
        return clientState.closedCS();
      }
  }

}



