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

package com.akkaserverless.javasdk.impl.eventsourcedentity

import com.akkaserverless.javasdk.eventsourcedentity._
import com.akkaserverless.testkit.TestProtocol
import com.akkaserverless.testkit.eventsourcedentity.EventSourcedMessages
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EventSourcedEntitiesImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  import EventSourcedEntitiesImplSpec._
  import EventSourcedMessages._
  import ShoppingCart.Item
  import ShoppingCart.Protocol._

  val service: TestEventSourcedService = ShoppingCart.testService
  val protocol: TestProtocol = TestProtocol(service.port)

  override def afterAll(): Unit = {
    protocol.terminate()
    service.terminate()
  }

  "EventSourcedImpl" should {

    "manage entities with expected commands and events" in {
      val entity = protocol.eventSourced.connect()
      entity.send(init(ShoppingCart.Name, "cart"))
      entity.send(command(1, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(1, EmptyCart))
      entity.send(command(2, "cart", "AddItem", addItem("abc", "apple", 1)))
      entity.expect(reply(2, EmptyJavaMessage, persist(itemAdded("abc", "apple", 1))))
      entity.send(command(3, "cart", "AddItem", addItem("abc", "apple", 2)))
      entity.expect(
        reply(3,
              EmptyJavaMessage,
              persist(itemAdded("abc", "apple", 2)).withSnapshot(cartSnapshot(Item("abc", "apple", 3))))
      )
      entity.send(command(4, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(4, cart(Item("abc", "apple", 3))))
      entity.send(command(5, "cart", "AddItem", addItem("123", "banana", 4)))
      entity.expect(reply(5, EmptyJavaMessage, persist(itemAdded("123", "banana", 4))))
      entity.passivate()
      val reactivated = protocol.eventSourced.connect()
      reactivated.send(init(ShoppingCart.Name, "cart", snapshot(3, cartSnapshot(Item("abc", "apple", 3)))))
      reactivated.send(event(4, itemAdded("123", "banana", 4)))
      reactivated.send(command(1, "cart", "GetCart", getShoppingCart("cart")))
      reactivated.expect(reply(1, cart(Item("abc", "apple", 3), Item("123", "banana", 4))))
      reactivated.passivate()
    }

    "handle emit of several events" in {
      val entity = protocol.eventSourced.connect()
      entity.send(init(ShoppingCart.Name, "cart"))
      entity.send(
        command(1,
                "cart",
                "AddItems",
                addItems(Item("abc", "apple", 1), Item("123", "banana", 4), Item("456", "pear", 2)))
      )
      entity.expect(
        reply(
          1,
          EmptyJavaMessage,
          persist(itemAdded("abc", "apple", 1), itemAdded("123", "banana", 4), itemAdded("456", "pear", 2))
          // note that snapshot every 2nd, but after all events and therefore including pear
            .withSnapshot(cartSnapshot(Item("abc", "apple", 1), Item("123", "banana", 4), Item("456", "pear", 2)))
        )
      )
      entity.send(command(2, "cart", "GetCart", getShoppingCart("cart")))
      entity.expect(reply(2, cart(Item("abc", "apple", 1), Item("123", "banana", 4), Item("456", "pear", 2))))
      entity.passivate()
    }

    "fail when first message is not init" in {
      service.expectLogError("Terminating entity due to unexpected failure") {
        val entity = protocol.eventSourced.connect()
        entity.send(command(1, "cart", "command"))
        val message = entity.expectMessage()
        val failure = message.failure.get
        failure.description should startWith("Protocol error: Expected init message for Event Sourced Entity")
        entity.expectClosed()
      }
    }

    "fail when service doesn't exist" in {
      service.expectLogError("Terminating entity [foo] due to unexpected failure") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(serviceName = "DoesNotExist", entityId = "foo"))
        entity.expect(failure("Protocol error: Service not found: DoesNotExist"))
        entity.expectClosed()
      }
    }

    "fail when command payload is missing" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [foo]") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(command(1, "cart", "foo", payload = None))
        entity.expect(failure(1, "Protocol error: No command payload"))
        entity.expectClosed()
      }
    }

    "fail when command entity id is incorrect" in {
      service.expectLogError("Terminating entity [cart2] due to unexpected failure for command [foo]") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart1"))
        entity.send(command(1, "cart2", "foo"))
        entity.expect(failure(1, "Protocol error: Receiving entity is not the intended recipient of command"))
        entity.expectClosed()
      }
    }

    "fail when entity is sent multiple init" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.expect(failure("Protocol error: Entity already inited"))
        entity.expectClosed()
      }
    }

    "fail when entity is sent empty message" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(EmptyInMessage)
        entity.expect(failure("Protocol error: Received empty/unknown message"))
        entity.expectClosed()
      }
    }

    "fail when event handler does not exist" in {
      service.expectLogError("Terminating entity due to unexpected failure") {
        val entity = protocol.eventSourced.connect()
        val notEvent = domainLineItem("?", "not an event", 1)
        val eventClass = notEvent.getClass
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(event(1, notEvent))
        entity.expect(failure(s"Unexpected failure: Unknown event type [$eventClass] on ${classOf[CartEntity]}"))
        entity.expectClosed()
      }
    }

    "fail when event handler throws exception" in {
      service.expectLogError("Terminating entity due to unexpected failure") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(event(1, itemAdded("123", "FAIL", 42)))
        entity.expect(failure("Unexpected failure: Boom: name is FAIL"))
        entity.expectClosed()
      }
    }

    "fail when command handler does not exist" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [foo]") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(command(1, "cart", "foo"))
        entity.expect(
          failure(
            1,
            s"No command handler found for command [foo] on ${classOf[CartEntity]}"
          )
        )
        entity.expectClosed()
      }
    }

    "fail action when command handler returns error effect" in {
      service.expectLogError(
        "Fail invoked for command [AddItem] for entity [cart]: Quantity for item foo must be greater than zero."
      ) {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(command(1, "cart", "AddItem", addItem("foo", "bar", -1)))
        entity.expect(actionFailure(1, "Quantity for item foo must be greater than zero."))
        entity.send(command(2, "cart", "GetCart", getShoppingCart("cart")))
        entity.expect(reply(2, EmptyCart)) // check entity state hasn't changed
        entity.passivate()
      }
    }

    "fail when command handler throws exception" in {
      service.expectLogError("Terminating entity [cart] due to unexpected failure for command [RemoveItem]") {
        val entity = protocol.eventSourced.connect()
        entity.send(init(ShoppingCart.Name, "cart"))
        entity.send(command(1, "cart", "RemoveItem", removeItem("foo")))
        entity.expect(
          failure(
            1,
            "Unexpected failure: java.lang.RuntimeException: Boom: foo"
          )
        )
        entity.expectClosed()
      }
    }
  }
}

object EventSourcedEntitiesImplSpec {
  object ShoppingCart {
    import com.example.shoppingcart.ShoppingCartApi
    import com.example.shoppingcart.domain.ShoppingCartDomain

    val Name: String = ShoppingCartApi.getDescriptor.findServiceByName("ShoppingCartService").getFullName

    def testService: TestEventSourcedService =
      TestEventSourced.service(
        CartEntityProvider
          .of(new CartEntity(_))
          .withOptions(EventSourcedEntityOptions.defaults().withSnapshotEvery(2))
      )

    case class Item(id: String, name: String, quantity: Int)

    object Protocol {
      import scala.jdk.CollectionConverters._

      val EmptyCart: ShoppingCartApi.Cart = ShoppingCartApi.Cart.newBuilder.build

      def cart(items: Item*): ShoppingCartApi.Cart =
        ShoppingCartApi.Cart.newBuilder.addAllItems(lineItems(items.sortBy(_.id))).build

      def lineItems(items: Seq[Item]): java.lang.Iterable[ShoppingCartApi.LineItem] =
        items.map(item => lineItem(item.id, item.name, item.quantity)).asJava

      def lineItem(id: String, name: String, quantity: Int): ShoppingCartApi.LineItem =
        ShoppingCartApi.LineItem.newBuilder.setProductId(id).setName(name).setQuantity(quantity).build

      def getShoppingCart(id: String): ShoppingCartApi.GetShoppingCart =
        ShoppingCartApi.GetShoppingCart.newBuilder.setCartId(id).build

      def addItem(id: String, name: String, quantity: Int): ShoppingCartApi.AddLineItem =
        ShoppingCartApi.AddLineItem.newBuilder.setProductId(id).setName(name).setQuantity(quantity).build

      def addItems(items: Item*): ShoppingCartApi.AddLineItems =
        ShoppingCartApi.AddLineItems.newBuilder.addAllItems(lineItems(items).asScala.toList.asJava).build

      def removeItem(id: String): ShoppingCartApi.RemoveLineItem =
        ShoppingCartApi.RemoveLineItem.newBuilder.setProductId(id).build

      def itemAdded(id: String, name: String, quantity: Int): ShoppingCartDomain.ItemAdded =
        ShoppingCartDomain.ItemAdded.newBuilder.setItem(domainLineItem(id, name, quantity)).build

      def domainLineItems(items: Seq[Item]): java.lang.Iterable[ShoppingCartDomain.LineItem] =
        items.sortBy(_.id).map(item => domainLineItem(item.id, item.name, item.quantity)).asJava

      def domainLineItem(id: String, name: String, quantity: Int): ShoppingCartDomain.LineItem =
        ShoppingCartDomain.LineItem.newBuilder.setProductId(id).setName(name).setQuantity(quantity).build

      def cartSnapshot(items: Item*): ShoppingCartDomain.Cart =
        ShoppingCartDomain.Cart.newBuilder.addAllItems(domainLineItems(items)).build
    }

  }
}
