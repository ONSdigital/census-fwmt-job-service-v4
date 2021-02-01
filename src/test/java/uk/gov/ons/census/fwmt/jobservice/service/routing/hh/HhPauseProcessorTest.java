package uk.gov.ons.census.fwmt.jobservice.service.routing.hh;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.census.fwmt.common.data.tm.CasePauseRequest;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.jobservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.jobservice.hh.HhRequestBuilder;
import uk.gov.ons.census.fwmt.jobservice.http.comet.CometRestClient;
import uk.gov.ons.census.fwmt.jobservice.service.GatewayCacheService;
import uk.gov.ons.census.fwmt.jobservice.service.routing.RoutingValidator;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HhPauseProcessorTest {

  @InjectMocks
  private HhPause hhPause;

  @Mock
  private CometRestClient cometRestClient;

  @Mock
  private GatewayCacheService cacheService;

  @Mock
  private GatewayCache gatewayCache;

  @Mock
  private GatewayEventManager eventManager;

  @Mock
  private CasePauseRequest casePauseRequest;

  @Mock
  private RoutingValidator routingValidator;

  @Captor
  private ArgumentCaptor<GatewayCache> spiedCache;

  @Test
  @DisplayName("Should save HH Pause as cancel")
  public void shouldSaveHhPauseAsCancel() throws GatewayException {
    final FwmtActionInstruction instruction = HhRequestBuilder.createPauseInstruction();
    GatewayCache gatewayCache = GatewayCache.builder()
        .caseId("ac623e62-4f4b-11eb-ae93-0242ac130002").build();
    when(cacheService.getById(anyString())).thenReturn(gatewayCache);
    ResponseEntity<Void> responseEntity = ResponseEntity.ok().build();
    when(cometRestClient.sendPause(any(CasePauseRequest.class), eq(instruction.getCaseId()))).thenReturn(responseEntity);
    hhPause.process(instruction, gatewayCache, Instant.now());
    verify(cacheService).save(spiedCache.capture());
    String lastActionInstruction = spiedCache.getValue().lastActionInstruction;
    Assertions.assertEquals("CANCEL", lastActionInstruction);
  }
}