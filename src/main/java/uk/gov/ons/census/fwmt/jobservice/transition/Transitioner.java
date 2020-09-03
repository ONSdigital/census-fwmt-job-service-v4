package uk.gov.ons.census.fwmt.jobservice.transition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtCancelActionInstruction;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.jobservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.jobservice.data.MessageCache;
import uk.gov.ons.census.fwmt.jobservice.service.MessageCacheService;
import uk.gov.ons.census.fwmt.jobservice.service.converter.TransitionRule;
import uk.gov.ons.census.fwmt.jobservice.service.processor.InboundProcessor;
import uk.gov.ons.census.fwmt.jobservice.transition.utils.CacheHeldMessages;
import uk.gov.ons.census.fwmt.jobservice.transition.utils.MergeMessages;
import uk.gov.ons.census.fwmt.jobservice.transition.utils.RetrieveTransitionRules;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.NO_ACTION_REQUIRED;
import static uk.gov.ons.census.fwmt.jobservice.config.GatewayEventsConfig.ROUTING_FAILED;


@Slf4j
@Component
public class Transitioner {
  @Autowired
  private GatewayEventManager eventManager;

  @Autowired
  private MessageCacheService messageCacheService;

  @Autowired
  private CacheHeldMessages cacheHeldMessages;

  @Autowired
  private RetrieveTransitionRules retrieveTransitionRules;

  @Autowired
  private MergeMessages mergeMessages;

  public void processTransition(GatewayCache cache, Object rmRequestReceived,
        InboundProcessor<?> processor, Date messageQueueTime) throws GatewayException {
    boolean isCancel = false;
    SimpleDateFormat reformatDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSSS");
    String actionInstruction;
    String caseId;
    String caseref;
    FwmtActionInstruction rmRequestCreateUpdate = null;
    FwmtCancelActionInstruction rmRequestCancel = null;
    InboundProcessor<FwmtActionInstruction> processorCreateUpdate = null;
    InboundProcessor<FwmtCancelActionInstruction> processorCancel = null;

    if (rmRequestReceived instanceof FwmtActionInstruction) {
      rmRequestCreateUpdate = (FwmtActionInstruction) rmRequestReceived;
      processorCreateUpdate = (InboundProcessor<FwmtActionInstruction>) processor;
      actionInstruction = rmRequestCreateUpdate.getActionInstruction().toString();
      caseId = rmRequestCreateUpdate.getCaseId();
      caseref = rmRequestCreateUpdate.getCaseRef();
    } else {
      rmRequestCancel = (FwmtCancelActionInstruction) rmRequestReceived;
      processorCancel = (InboundProcessor<FwmtCancelActionInstruction>) processor;
      actionInstruction = rmRequestCancel.getActionInstruction().toString();
      caseId = rmRequestCancel.getCaseId();
      caseref = "";
      isCancel = true;
    }

    if (messageQueueTime != null) {
      try {
        messageQueueTime = reformatDate.parse(messageQueueTime.toString());
      } catch (ParseException e) {
        throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "Could not format message date", caseId);
      }
    } else {
      throw new GatewayException(GatewayException.Fault.VALIDATION_FAILED, "Message did not include a timestamp", caseId);
    }

    TransitionRule returnedRules = retrieveTransitionRules
        .collectTransitionRules(cache, actionInstruction, caseId, messageQueueTime);

    MessageCache messageCache = messageCacheService.getById(caseId);

    switch (returnedRules.getAction()) {
      case NO_ACTION:
        eventManager
            .triggerEvent(caseId, NO_ACTION_REQUIRED,
                "Case Ref", caseref);
        break;
      case REJECT:
        eventManager.triggerErrorEvent(this.getClass(), "Request from RM rejected",
            String.valueOf(caseId), ROUTING_FAILED);
        break;
      case PROCESS:
        if (isCancel) {
          processorCancel.process(rmRequestCancel, cache, messageQueueTime);
        } else {
          processorCreateUpdate.process(rmRequestCreateUpdate, cache, messageQueueTime);
        }
        break;
      case MERGE:
        if (!isCancel) {
          processorCreateUpdate.process(rmRequestCreateUpdate, cache, messageQueueTime);
        }
        mergeMessages.mergeRecords(messageCache);
        break;
      default:
        throw new GatewayException(GatewayException.Fault.VALIDATION_FAILED, "No such transition rule", caseId);
    }

  switch (returnedRules.getRequestAction()) {
    case SAVE:
      if (isCancel) {
        cacheHeldMessages.cacheMessage(messageCache, cache, rmRequestCancel, messageQueueTime);
      } else {
        cacheHeldMessages.cacheMessage(messageCache, cache, rmRequestCreateUpdate, messageQueueTime);
      }
      break;
    case CLEAR:
      if (messageCache != null) {
        messageCacheService.delete(messageCache);
      }
      break;
    default:
      break;
    }
  }
}