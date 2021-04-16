package uk.swarts.training.spring.webclient.address;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.swarts.training.spring.webclient.exception.ClientException;

@Component
public class AddressClient {

  private final WebClient webClient;
  private final AddressProperties addressProperties;

  public AddressClient(WebClient webClient,
      AddressProperties addressProperties) {
    this.webClient = webClient;
    this.addressProperties = addressProperties;
  }

  public Flux<Address> getAll() {
    return webClient.get()
        .uri(addressProperties.getPathAddresses())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> Mono.just(ClientException.from(response)))
        .bodyToFlux(Address.class);
  }

  public Mono<Address> get(String addressId) {
    return webClient.get()
        .uri(addressProperties.getPathAddress(), addressId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> Mono.just(ClientException.from(response)))
        .bodyToMono(Address.class);
  }

  public Mono<Address> create(Address address) {
    return webClient.post()
        .uri(addressProperties.getPathAddresses())
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(address)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> Mono.just(ClientException.from(response)))
        .bodyToMono(Address.class);
  }

  public Mono<Void> delete(String addressId) {
    return webClient.delete()
        .uri(addressProperties.getPathAddress(), addressId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> Mono.just(ClientException.from(response)))
        .bodyToMono(Void.class);
  }
}
