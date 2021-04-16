package uk.swarts.training.spring.webclient.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Address {

  @JsonProperty("address_id")
  private String id;

  private String line1;

  private String line2;

  private String line3;

  @JsonProperty("post_code")
  private String postCode;

  private String town;

  private String country;
}
