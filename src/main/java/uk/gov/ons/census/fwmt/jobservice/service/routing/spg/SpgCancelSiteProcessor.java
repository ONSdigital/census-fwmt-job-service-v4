package uk.gov.ons.census.fwmt.jobservice.service.routing.spg;

import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.COMET_CANCEL_ACK;
import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.COMET_CANCEL_PRE_SENDING;
import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.FAILED_TO_CREATE_TM_JOB;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtCancelActionInstruction;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.jobservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.jobservice.http.comet.CometRestClient;
import uk.gov.ons.census.fwmt.jobservice.service.processor.InboundProcessor;
import uk.gov.ons.census.fwmt.jobservice.service.processor.ProcessorKey;
import uk.gov.ons.census.fwmt.jobservice.service.routing.RoutingValidator;

@Qualifier("Cancel")
@Component
public class SpgCancelSiteProcessor implements InboundProcessor<FwmtCancelActionInstruction> {

  @Autowired
  private CometRestClient cometRestClient;

  @Autowired
  private GatewayEventManager eventManager;

  @Autowired
  private RoutingValidator routingValidator;

  private static ProcessorKey key = ProcessorKey.builder()
  .actionInstruction(ActionInstructionType.CANCEL.toString())
  .surveyName("CENSUS")
  .addressType("SPG")
  .addressLevel("E")
  .build();

  @Override
  public ProcessorKey getKey() {
    return key;
  }

  // TODO Remove format on save
  // TODO add ffa formatter (modify)
  // TODO Find ignore foramtting tag
  // TODO Make eventManager Anotation
  @Override
  public boolean isValid(FwmtCancelActionInstruction rmRequest, GatewayCache cache) {
    try {
      // relies on the validation of: SpgRouter, SpgCancelRouter
      return rmRequest.getActionInstruction() == ActionInstructionType.CANCEL
          && rmRequest.getSurveyName().equals("CENSUS") && rmRequest.getAddressType().equals("SPG")
          && rmRequest.getAddressLevel().equals("E") && cache != null;
    } catch (NullPointerException e) {
      return false;
    }
  }

  // TODO Acceptance test should check delete is sent (new event)
  // TODO Can ecent be added in class where its used, rather than config, or csan
  // it be added when used firat time
  @Override
  public void process(FwmtCancelActionInstruction rmRequest, GatewayCache cache) throws GatewayException {
    eventManager.triggerEvent(String.valueOf(rmRequest.getCaseId()), COMET_CANCEL_PRE_SENDING, "Case Ref", "N/A");
    ResponseEntity<Void> response = cometRestClient.sendDelete(rmRequest.getCaseId());
    routingValidator.validateResponseCode(response, rmRequest.getCaseId(), "Cancel", FAILED_TO_CREATE_TM_JOB);
    eventManager.triggerEvent(String.valueOf(rmRequest.getCaseId()), COMET_CANCEL_ACK, "Case Ref", "N/A",
        "Response Code", response.getStatusCode().name());
  }
}
