package uk.gov.ons.fwmt.census.jobservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.fwmt.census.jobservice.message.impl.RMProducerImpl;
import uk.gov.ons.fwmt.census.jobservice.rest.client.impl.CometRestClientImpl;
import uk.gov.ons.fwmt.census.jobservice.service.JobService;
import uk.gov.ons.fwmt.census.jobservice.tm.client.TMClient;
import uk.gov.ons.fwmt.fwmtgatewaycommon.data.FWMTCancelJobRequest;
import uk.gov.ons.fwmt.fwmtgatewaycommon.data.FWMTCreateJobRequest;
import uk.gov.ons.fwmt.fwmtgatewaycommon.error.CTPException;
import uk.gov.ons.fwmt.fwmtohsjobstatusnotification.FwmtOHSJobStatusNotification;

@Service
public class JobServiceImpl implements JobService {
  @Autowired
  private TMClient tmJobService;

  @Autowired
  private RMProducerImpl rmProducer;

  @Autowired
  private CometRestClientImpl cometRestClient;

  @Override public void createJob(FWMTCreateJobRequest jobRequest) throws CTPException {
    cometRestClient.convertAndSendCreate(jobRequest);
  }

  @Override public void cancelJob(FWMTCancelJobRequest cancelRequest) {
    tmJobService.cancelJob(cancelRequest);
  }

  @Override public void notifyRM(FwmtOHSJobStatusNotification response) throws CTPException {
    rmProducer.send(response);

  }
}
