package uk.gov.ons.fwmt.census.jobservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.gov.ons.fwmt.census.jobservice.comet.dto.ModelCase;
import uk.gov.ons.fwmt.census.jobservice.converter.CometConverter;
import uk.gov.ons.fwmt.census.jobservice.message.impl.RMProducerImpl;
import uk.gov.ons.fwmt.census.jobservice.rest.client.impl.CometRestClientImpl;
import uk.gov.ons.fwmt.census.jobservice.service.JobService;
import uk.gov.ons.fwmt.census.jobservice.tm.client.TMClient;
import uk.gov.ons.fwmt.fwmtgatewaycommon.data.FWMTCancelJobRequest;
import uk.gov.ons.fwmt.fwmtgatewaycommon.data.FWMTCreateJobRequest;
import uk.gov.ons.fwmt.fwmtgatewaycommon.error.CTPException;
import uk.gov.ons.fwmt.fwmtohsjobstatusnotification.FwmtOHSJobStatusNotification;

import java.util.Map;

@Service
public class JobServiceImpl implements JobService {
  @Autowired
  private TMClient tmJobService;

  @Autowired
  private RMProducerImpl rmProducer;

  @Autowired
  private CometRestClientImpl cometRestClient;

  @Autowired
  private Map<String, CometConverter> cometConverters;

  @Override
  public void createJob(FWMTCreateJobRequest jobRequest) throws CTPException {
    convertAndSendCreate(jobRequest);
  }

  @Override
  public void cancelJob(FWMTCancelJobRequest cancelRequest) {
    tmJobService.cancelJob(cancelRequest);
  }


  @Override
  public void notifyRM(FwmtOHSJobStatusNotification response) throws CTPException {
    rmProducer.send(response);

  }

  @Override
  public void convertAndSendCreate(FWMTCreateJobRequest jobRequest) throws CTPException {
    final CometConverter cometConverter = cometConverters.get(jobRequest.getSurveyType());
    ModelCase modelCase = cometConverter.convert(jobRequest);
    cometRestClient.sendCreateJobRequest(modelCase);
  }

}