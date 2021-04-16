# Testing Spring Boot WebClient with MockWebServer

This is a demo application to show how to use `WebClient` and test it using `MockWebServer`.

In this application we implemented `MockServer` wrapper class to test `WebClient` API call in an easy and functional way.
It uses `MockWebServer` from `okthttp` library.

The application calls `address-service` endpoints for the demonstration.

## WebClient

Spring introduced reactive web framework called WebFlux in Spring 5.
It comes with `WebClient` is the new reactive asynchronous/non-blocking HTTP client with a fluent functional style API.
In order to use it you should add `spring-webflux` module to your project.

- For the gradle project add following dependency to `gradle.build`
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
}
```


## Demo Application
In the demo application a 3rd party service `address-service` apis are called.
`address-service` has APIs to allow us to manage and get address information. Our demo application requires these address information for the customer.
It manages and get customers' address information through the 3rd party APIs.

`address-service` configuration is added to the `application.yml` file.

```yaml
address-service:
  host: http://localhost:8081
  path-addresses: ${address-service.host}/address
  path-address: ${address-service.host}/address/{addressId}
```

In demo application:

- `AddressClient` is the client implementation to call `address-service` apis using WebClient.

```java
public class AddressClient {

  private final WebClient webClient;
  private final AddressProperties addressProperties;

  public AddressClient(WebClient webClient,
    AddressProperties addressProperties) {
    this.webClient = webClient;
    this.addressProperties = addressProperties;
  }


  public Flux<Address> getAll() {
  // ...
  }

  public Mono<Address> get(String addressId) {
  // ...
  }

  public Mono<Address> create(Address address) {
  // ...
  }

  public Mono<Void> delete(String addressId) {
  // ...
  }
}
```

- WebClient created as a bean:
```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    .build();
}
```
We could set default and common properties in this bean for the WebClient
that could be used for the other client implementations for the other 3rd party calls.


In the demo application GET, POST, DELETE operations implemented.

- GET
```java

  public Mono<Address> get(String addressId) {
    return webClient.get()
        .uri(addressProperties.getPathAddress(), addressId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> Mono.just(ClientException.from(response)))
        .bodyToMono(Address.class);
  }
```

- POST

```java
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
```

- DELETE

```java
  public Mono<Void> delete(String addressId) {
    return webClient.delete()
        .uri(addressProperties.getPathAddress(), addressId)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(HttpStatus::isError, response -> Mono.just(ClientException.from(response)))
        .bodyToMono(Void.class);
  }
```

## Testing with MockServer

In this project we implemented `MockServer` that wrap HTTP client mocking operations and allow us to implement test cases in an easy-way.


### MockWebServer

MockWebServer is a web server that can receive and respond to HTTP requests.

It is developed by the Square team.

MockWebServer help us to use real HTTP calls to a local endpoint, and allow us to mock a complex client api calls.

- For the gradle project add following dependency to `gradle.build`
```groovy
dependencies {
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.9.1'
    testImplementation 'com.squareup.okhttp3:okhttp:4.9.1'
}
```

### Implementation

MockServer wrap `MockWebServer` to prepare desired response for a given request.

It wraps MockWebServer start and shutdown operations. 
So, in the test class before all tests `create` and after all tests `dispose` method should be called.

It has `RequestVerifier` and `ResponseVerifier` to validate the request and the response.

```java
public class MockServer {

  private final MockWebServer server;

  private MockServer() {
    this.server = new MockWebServer();
  }

  public static MockServer create() {
    return new MockServer();
  }

  public void dispose() throws IOException {
    server.shutdown();
  }

  public MockServer responseWith(HttpStatus status) {
    // ...
  }

  public <T> MockServer responseWith(HttpStatus status, T responseBody,
      Map<String, String> headers) {
    MockResponse response = new MockResponse()
        .setResponseCode(status.value())
        .setBody(toJson(responseBody));
    headers.forEach(response::addHeader);

    server.enqueue(response);

    return this;
  }
  

  public <T> ResponseVerifier call(ClientDelegate<T> clientDelegate) {
    // ...
  }

  public RequestVerifier takeRequest() {
    // ...
  }

  public void clearRequest() {
    // ...
  }

  // ...
}
```

MockServer Methods:

- `responseWith`: It enqueues the mocked response. You could arrange desired http status, body and headers.
- `call`: It is a delegate to call your client method. It returns a ResponseVerifier after the API call, and you could check the response.
- `takeRequest`: It returns the request that sent to the 3rd party API. It returns a RequestVerifier and allows you to check the request.
- `clearRequest`: If you do not want to check the request, call it to delete the request from the queue.
  
RequestVerifier Methods:
- `expectPath`: Check the `path` 
- `expectHeader`: Check header value with `name` and `value`
- `expectMethod`: Check Http method like POST, DELETE, PUT, ... etc.
- `expectBody`: Check request body object.


ResponseVerifier Methods:

- `expectClientError`: Check the response if it is a 4xx client error.
- `expectServerError`: Check the response if it is a 5xx server error.
- `expectResponse`: Check the expected response body if it is equal to the `response`.
- `expectArrayResponse`: Check the expected response if it is equal to the `responseArray`.
- `expectNoContent`: Expect empty response body.


### Usage

AddressClientTest is used to the AddressClient.

Here you can see the fields, constants and setup functions.

`mockServer` is defined as a static attribute, its create operation is called before all test and 
dispose operation called after all tests.

```java
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
  // ...
  
}

```

We explained the `mockServer` methods as a comment in below samples for different HTTP operation and responses.

- Call and validate GET operation
  
```java
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
```

1. `responseWith(HttpStatus.OK, addressResponse, headers)` → prepares desired response from the 3rd party API call
2. `call(() -> addressClient.get(addressId))` → calls addressClient to get address method with addressId
3. `expectResponse(addressResponse)` → checks get address method response
4. `takeRequest()` → takes request that sent to the 3rd party service, after this point you could verify only the request
5. `expectHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)` → checks request has the expected accept header value
6. `expectMethod(HttpMethod.GET.name())` → checks if the request is a GET operation
7. `expectPath(expectedPath)` → validates the request path

- Call and validate POST operation

```java

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
```

1. `responseWith(HttpStatus.CREATED, addressResponse, headers)` → prepares desired response from the 3rd party API call
2. `call(() -> addressClient.create(addressRequest))` → calls addressClient create address method with addressId
3. `expectResponse(addressResponse)` → check create address method response
4. `expectBody(addressRequest, Address.class)` → validate request address

- Call and validate DELETE operation
  
```java
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
```


- Validate client error.
```java 
  @Test
  void deleteAddressShouldReturnsClientErrorWhenServerRespondsWith4xxError() {
      mockServer.responseWith(HttpStatus.BAD_REQUEST)  
      .call(() -> addressClient.delete("address-2"))
      .expectClientError()                                         
      .clearRequest();                                
  }
```

1. `responseWith(HttpStatus.BAD_REQUEST)` → client returns 400 error
2. `expectClientError()` → validate if delete method maps this error to the correct ClientException
3. `clearRequest()` → clear request from the queue

- Validate server error.
```java 
  @Test
  void deleteAddressShouldReturnsServerErrorWhenServerRespondsWith5xxError() {
      mockServer.responseWith(HttpStatus.INTERNAL_SERVER_ERROR) 
      .call(() -> addressClient.delete("address-3"))
      .expectServerError()                                      
      .clearRequest();
   }

```

1. `responseWith(HttpStatus.INTERNAL_SERVER_ERROR)` → 3rd party client returns 500 error
2. `expectServerError()` → validate if delete method maps this error to the correct ClientException





