package uk.gov.ons.census.fwmt.jobservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.jobservice.Application;

@Configuration
public class GatewayEventsConfig {

  public static final String CANONICAL_CREATE_JOB_RECEIVED = "CANONICAL_CREATE_JOB_RECEIVED";
  public static final String CANONICAL_CANCEL_RECEIVED = "CANONICAL_CANCEL_RECEIVED";
  public static final String CANONICAL_UPDATE_RECEIVED = "CANONICAL_UPDATE_RECEIVED";
  public static final String COMET_CREATE_SENT = "COMET_CREATE_SENT";
  public static final String COMET_CREATE_ACK = "COMET_CREATE_ACK";
  public static final String COMET_CANCEL_SENT = "COMET_CANCEL_SENT";
  public static final String COMET_CANCEL_ACK = "COMET_CANCEL_ACK";
  public static final String COMET_UPDATE_SENT = "COMET_UPDATE_SENT";
  public static final String COMET_UPDATE_ACK = "COMET_UPDATE_ACK";

  public static final String FAILED_TO_UNMARSHALL_CANONICAL = "FAILED_TO_UNMARSHALL_CANONICAL";
  public static final String INVALID_CANONICAL_ACTION = "INVALID_CANONICAL_ACTION";
  public static final String FAILED_TM_AUTHENTICATION = "FAILED_TM_AUTHENTICATION";

  @Bean
  public GatewayEventManager gatewayEventManager() {
    GatewayEventManager gatewayEventManager = new GatewayEventManager();
    gatewayEventManager.setSource(Application.APPLICATION_NAME);
    gatewayEventManager.addEventTypes(new String[] {CANONICAL_CREATE_JOB_RECEIVED, CANONICAL_CANCEL_RECEIVED,
        CANONICAL_UPDATE_RECEIVED, COMET_CREATE_SENT, COMET_CREATE_ACK, COMET_CANCEL_SENT, COMET_CANCEL_ACK,
        COMET_UPDATE_SENT, COMET_UPDATE_ACK});
    gatewayEventManager.addErrorEventTypes(new String[] {FAILED_TO_UNMARSHALL_CANONICAL, INVALID_CANONICAL_ACTION, FAILED_TM_AUTHENTICATION});

    return gatewayEventManager;
  }
}
