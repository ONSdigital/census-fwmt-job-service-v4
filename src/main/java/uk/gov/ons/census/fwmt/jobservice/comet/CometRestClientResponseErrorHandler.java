package uk.gov.ons.census.fwmt.jobservice.comet;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CometRestClientResponseErrorHandler implements ResponseErrorHandler {

  @Override
  public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
    return httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError();
  }

  @Override
  public void handleError(ClientHttpResponse httpResponse) throws IOException {
    String body = new String(httpResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
    String message = "(" + httpResponse.getStatusCode().toString() + ") " + body;
    log.error(message);
    throw new RuntimeException(message);
  }

}
