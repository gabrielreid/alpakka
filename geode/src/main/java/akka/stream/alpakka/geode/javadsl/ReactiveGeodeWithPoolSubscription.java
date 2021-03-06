/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.geode.javadsl;

import akka.Done;
import akka.stream.alpakka.geode.AkkaPdxSerializer;
import akka.stream.alpakka.geode.GeodeSettings;
import akka.stream.alpakka.geode.impl.stage.GeodeContinuousSourceStage;
import akka.stream.javadsl.Source;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.query.CqException;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.QueryService;
import scala.Symbol;
import scala.concurrent.Future;

public
/** Reactive geode with server event subscription. Can build continuous source. */
class ReactiveGeodeWithPoolSubscription extends ReactiveGeode {

  /**
   * Subscribes to server event.
   *
   * @return ClientCacheFactory with server event subscription.
   */
  public final ClientCacheFactory configure(ClientCacheFactory factory) {
    return super.configure(factory).setPoolSubscriptionEnabled(true);
  }

  public ReactiveGeodeWithPoolSubscription(GeodeSettings settings) {
    super(settings);
  }

  public <V> Source<V, Future<Done>> continuousQuery(
      String queryName, String query, AkkaPdxSerializer<V> serializer) {

    registerPDXSerializer(serializer, serializer.clazz());
    return Source.fromGraph(
        new GeodeContinuousSourceStage<V>(cache(), Symbol.apply(queryName), query));
  }

  public boolean closeContinuousQuery(String name) throws CqException {

    QueryService qs = cache().getQueryService();
    CqQuery query = qs.getCq(name);
    if (query == null) return false;
    query.close();
    return true;
  }
}
