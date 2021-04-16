package uk.swarts.training.spring.webclient.address;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.swarts.training.spring.webclient.mockserver.MockServer;

class AddressClientTest {

  private static final String ADDRESSES_PATH = "/address";
  private static final String ADDRESS_PATH = "/address/{addressId}";
  private static MockServer mockServer;
  private static AddressProperties addressProperties;
  private AddressClient addressClient;

  @BeforeAll
  static void beforeAll() {
    mockServer = MockServer.create();
    addressProperties = AddressProperties.builder()
        .pathAddresses(mockServer.getMockServerUrl() + ADDRESSES_PATH)
        .pathAddress(mockServer.getMockServerUrl() + ADDRESS_PATH)
        .build();
  }

  @AfterAll
  static void afterAll() throws IOException {
    mockServer.dispose();
  }

  @BeforeEach
  void setup() {
    addressClient = new AddressClient(mockServer.getWebClient(), addressProperties);
  }

  @Test
  void getAddressesShouldRequestCorrectPathAndRetrieveAllAddress() {
    final Map<String, String> headers = Collections
        .singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    List<Address> addressList = Arrays.asList(
        Address.builder().id("address-1").build(),
        Address.builder().id("address-2").build()
    );

    mockServer.responseWith(HttpStatus.OK, addressList, headers)
        .call(() -> addressClient.getAll())
        .expectArrayResponse(addressList.toArray())
        .takeRequest()
        .expectHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .expectPath(ADDRESSES_PATH)
        .expectMethod(HttpMethod.GET.name());
  }

  @Test
  void getAddressesShouldReturnsClientErrorWhenServerRespondsWith4xxError() {
    mockServer.responseWith(HttpStatus.BAD_REQUEST)
        .call(() -> addressClient.getAll())
        .expectClientError()
        .clearRequest();
  }

  @Test
  void getAddressesShouldReturnsServerErrorWhenServerRespondsWith5xxError() {
    mockServer.responseWith(HttpStatus.INTERNAL_SERVER_ERROR)
        .call(() -> addressClient.getAll())
        .expectServerError()
        .clearRequest();
  }

  @Test
  void getAddressShouldRequestCorrectPathAndRetrieveAddress() {
    final Map<String, String> headers = Collections
        .singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    final String addressId = "address-1";

    Address addressResponse = Address.builder()
        .id(addressId)
        .build();

    final String expectedPath = ADDRESS_PATH.replace("{addressId}", addressId);

    mockServer.responseWith(HttpStatus.OK, addressResponse, headers)
        .call(() -> addressClient.get(addressId))
        .expectResponse(addressResponse)
        .takeRequest()
        .expectHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .expectMethod(HttpMethod.GET.name())
        .expectPath(expectedPath);
  }

  @Test
  void getAddressShouldReturnsClientErrorWhenServerRespondsWith4xxError() {
    mockServer.responseWith(HttpStatus.BAD_REQUEST)
        .call(() -> addressClient.get("address-2"))
        .expectClientError()
        .clearRequest();
  }

  @Test
  void getAddressShouldReturnsServerErrorWhenServerRespondsWith5xxError() {
    mockServer.responseWith(HttpStatus.INTERNAL_SERVER_ERROR)
        .call(() -> addressClient.get("address-3"))
        .expectServerError()
        .clearRequest();
  }

  @Test
  void createAddressShouldCreateRequestedAddress() throws JsonProcessingException {
    final Map<String, String> headers = Collections
        .singletonMap(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    Address addressRequest = Address.builder()
        .line1("line1")
        .line2("line2")
        .postCode("PC1 2NB")
        .town("London")
        .country("UK")
        .build();

    Address addressResponse = addressRequest.toBuilder()
        .id("address-1")
        .build();

    mockServer.responseWith(HttpStatus.CREATED, addressResponse, headers)
        .call(() -> addressClient.create(addressRequest))
        .expectResponse(addressResponse)
        .takeRequest()
        .expectBody(addressRequest, Address.class)
        .expectHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .expectHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .expectMethod(HttpMethod.POST.name())
        .expectPath(ADDRESSES_PATH);
  }

  @Test
  void createAddressShouldReturnsClientErrorWhenServerRespondsWith4xxError() {
    mockServer.responseWith(HttpStatus.BAD_REQUEST)
        .call(() -> addressClient.create(Address.builder().build()))
        .expectClientError()
        .clearRequest();
  }

  @Test
  void createAddressShouldReturnsServerErrorWhenServerRespondsWith5xxError() {
    mockServer.responseWith(HttpStatus.INTERNAL_SERVER_ERROR)
        .call(() -> addressClient.create(Address.builder().build()))
        .expectServerError()
        .clearRequest();
  }

  @Test
  void deleteAddressShouldDeleteRequestedAddress() {

    final String addressId = "address-1";

    final String expectedPath = ADDRESS_PATH
        .replace("{addressId}", addressId);

    mockServer.responseWith(HttpStatus.NO_CONTENT)
        .call(() -> addressClient.delete(addressId))
        .expectNoContent()
        .takeRequest()
        .expectMethod(HttpMethod.DELETE.name())
        .expectPath(expectedPath);
  }

  @Test
  void deleteAddressShouldReturnsClientErrorWhenServerRespondsWith4xxError() {
    mockServer.responseWith(HttpStatus.BAD_REQUEST)
        .call(() -> addressClient.delete("address-2"))
        .expectClientError()
        .clearRequest();
  }

  @Test
  void deleteAddressShouldReturnsServerErrorWhenServerRespondsWith5xxError() {
    mockServer.responseWith(HttpStatus.INTERNAL_SERVER_ERROR)
        .call(() -> addressClient.delete("address-3"))
        .expectServerError()
        .clearRequest();
  }
}
