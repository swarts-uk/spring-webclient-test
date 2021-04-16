package uk.swarts.training.spring.webclient.mockserver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import org.reactivestreams.Publisher;
import reactor.test.StepVerifier;
import uk.swarts.training.spring.webclient.exception.ClientException;

public class ResponseVerifier {

  private final Publisher<?> publisher;
  private final MockServer mockServer;

  public ResponseVerifier(MockServer mockServer, Publisher<?> publisher) {
    assertThat("Publisher is null", publisher, is(notNullValue()));
    this.publisher = publisher;
    this.mockServer = mockServer;
  }

  public MockServer expectClientError() {
    StepVerifier.create(publisher)
        .expectErrorMatches(e -> {
          assertThat(e, instanceOf(ClientException.class));
          assertThat(((ClientException) e).isClientError(), is(true));
          return true;
        })
        .verify();
    return mockServer;
  }

  public MockServer expectServerError() {
    StepVerifier.create(publisher)
        .expectErrorMatches(e -> {
          assertThat(e, instanceOf(ClientException.class));
          assertThat(((ClientException) e).isServerError(), is(true));
          return true;
        })
        .verify();
    return mockServer;
  }

  public <T> MockServer expectResponse(T response) {
    StepVerifier.create((Publisher<T>) publisher)
        .expectNext(response)
        .verifyComplete();
    return mockServer;
  }

  public <T> MockServer expectArrayResponse(T[] responseArray) {
    StepVerifier.create((Publisher<T>) publisher)
        .expectNext(responseArray)
        .verifyComplete();
    return mockServer;
  }

  public MockServer expectNoContent() {
    StepVerifier.create(publisher)
        .verifyComplete();
    return mockServer;
  }
}
