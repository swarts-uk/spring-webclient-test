package uk.swarts.training.spring.webclient.mockserver;

import org.reactivestreams.Publisher;

@FunctionalInterface
public interface ClientDelegate<T> {

  Publisher<T> call();
}