package uk.swarts.training.spring.webclient.address;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Builder
@Configuration
@ConfigurationProperties(prefix = "address-service")
public class AddressProperties {

  private String pathAddresses;
  private String pathAddress;
}
