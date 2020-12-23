package uk.gov.ons.census.fwmt.jobservice.service.routing.nc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fwmt.common.data.nc.RefusalTypeDTO;
import uk.gov.ons.census.fwmt.common.data.tm.CaseRequest;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.jobservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.jobservice.http.comet.CometRestClient;
import uk.gov.ons.census.fwmt.jobservice.http.rm.RmRestClient;
import uk.gov.ons.census.fwmt.jobservice.nc.utils.NamedHouseholderRetrieval;
import uk.gov.ons.census.fwmt.common.data.nc.CaseDetailsDTO;
import uk.gov.ons.census.fwmt.jobservice.service.GatewayCacheService;
import uk.gov.ons.census.fwmt.jobservice.service.converter.nc.NcCreateConverter;
import uk.gov.ons.census.fwmt.jobservice.service.processor.InboundProcessor;
import uk.gov.ons.census.fwmt.jobservice.service.processor.ProcessorKey;
import uk.gov.ons.census.fwmt.jobservice.service.routing.RoutingValidator;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.COMET_CREATE_ACK;
import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.COMET_CREATE_PRE_SENDING;
import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.FAILED_TO_CREATE_TM_JOB;

@Qualifier("Create")
@Service
public class NcHhCreateEnglandAndWales implements InboundProcessor<FwmtActionInstruction> {

  private static final ProcessorKey key = ProcessorKey.builder()
      .actionInstruction(ActionInstructionType.CREATE.toString())
      .surveyName("CENSUS")
      .addressType("HH")
      .addressLevel("U")
      .build();

  @Autowired
  private CometRestClient cometRestClient;

  @Autowired
  private GatewayEventManager eventManager;

  @Autowired
  private RmRestClient rmRestClient;

  @Autowired
  private RoutingValidator routingValidator;

  @Autowired
  private GatewayCacheService cacheService;

  @Autowired
  private NamedHouseholderRetrieval namedHouseholderRetrieval;

  @Override
  public ProcessorKey getKey() {
    return key;
  }

  @Override
  public boolean isValid(FwmtActionInstruction rmRequest, GatewayCache cache) {
    try {
      return rmRequest.getActionInstruction() == ActionInstructionType.CREATE
          && rmRequest.getSurveyName().equals("CENSUS")
          && !rmRequest.getOa().startsWith("N")
          && rmRequest.isNc();
    } catch (NullPointerException e) {
      return false;
    }
  }

  @Override
  public void process(FwmtActionInstruction rmRequest, GatewayCache cache, Instant messageReceivedTime)
      throws GatewayException {
    CaseDetailsDTO houseHolderDetails;
    String accessInfo = null;
    String careCodes = null;
    String householder = "";

    try {
      houseHolderDetails = rmRestClient.getCase(rmRequest.getCaseId());
    } catch (RuntimeException e) {
      houseHolderDetails = null;
    }

    String newCaseId = String.valueOf(UUID.randomUUID());
    if (houseHolderDetails != null && houseHolderDetails.getRefusalReceived().equals(RefusalTypeDTO.HARD_REFUSAL)) {
      householder = namedHouseholderRetrieval.getAndSortRmRefusalCases(rmRequest.getCaseId(), houseHolderDetails);
    }

    CaseRequest tmRequest = NcCreateConverter.convertNcEnglandAndWales(rmRequest, cache, householder);

    eventManager.triggerEvent(newCaseId, COMET_CREATE_PRE_SENDING,
        "Case Ref", tmRequest.getReference(),
        "Original case id", rmRequest.getCaseId(),
        "Survey Type", tmRequest.getSurveyType().toString());

    ResponseEntity<Void> response = cometRestClient.sendCreate(tmRequest, newCaseId);
    routingValidator.validateResponseCode(response, newCaseId,
        "Create", FAILED_TO_CREATE_TM_JOB,
        "tmRequest", tmRequest.toString(),
        "rmRequest", rmRequest.toString(),
        "cache", (cache != null) ? cache.toString() : "");

    GatewayCache newCache = cacheService.getById(newCaseId);

    if (cache != null) {
      careCodes =  cache.getCareCodes();
      accessInfo = cache.getAccessInfo();
    }

    if (newCache == null) {
      cacheService.save(GatewayCache
          .builder()
          .caseId(newCaseId)
          .originalCaseId(rmRequest.getCaseId())
          .existsInFwmt(true)
          .type(10)
          .careCodes(careCodes)
          .accessInfo(accessInfo)
          .lastActionInstruction(rmRequest.getActionInstruction().toString())
          .lastActionTime(messageReceivedTime)
          .build());
    }

    eventManager
        .triggerEvent(newCaseId, COMET_CREATE_ACK,
            "Original case id", rmRequest.getCaseId(),
            "Case Ref", rmRequest.getCaseRef(),
            "Response Code", response.getStatusCode().name(),
            "Survey Type", tmRequest.getSurveyType().toString());
  }
}
