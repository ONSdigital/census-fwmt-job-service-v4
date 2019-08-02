package uk.gov.ons.census.fwmt.jobservice.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.canonical.v1.CancelFieldWorkerJobRequest;
import uk.gov.ons.census.fwmt.canonical.v1.CreateFieldWorkerJobRequest;
import uk.gov.ons.census.fwmt.canonical.v1.UpdateFieldWorkerJobRequest;
import uk.gov.ons.census.fwmt.common.data.modelcase.CasePauseRequest;
import uk.gov.ons.census.fwmt.common.data.modelcase.CaseRequest;
import uk.gov.ons.census.fwmt.common.data.modelcase.CcsCaseExtension;
import uk.gov.ons.census.fwmt.common.data.modelcase.Contact;
import uk.gov.ons.census.fwmt.common.data.modelcase.Location;
import uk.gov.ons.census.fwmt.common.data.modelcase.ModelCase;
import uk.gov.ons.census.fwmt.jobservice.converter.CometConverter;
import uk.gov.ons.census.fwmt.jobservice.entity.CCSOutcomeEntity;
import uk.gov.ons.census.fwmt.jobservice.service.JobCacheManager;

import static uk.gov.ons.census.fwmt.jobservice.utils.JobServiceUtils.setAddress;

@Component("CCS")
public class CCSIVConverter implements CometConverter {

  @Autowired
  private JobCacheManager jobCacheManager;

  @Override
  public CaseRequest convert(CreateFieldWorkerJobRequest ingest) {
    CcsCaseExtension ccsCaseExtension = new CcsCaseExtension();
    CaseRequest caseRequest = new CaseRequest();
    Location location = new Location();

    caseRequest.setReference(ingest.getCaseReference());
    caseRequest.setType(CaseRequest.TypeEnum.CCSIV);
    caseRequest.setSurveyType(ingest.getSurveyType());
    caseRequest.setEstabType(ingest.getEstablishmentType());
    // unsure of this one
    caseRequest.setRequiredOfficer(ingest.getMandatoryResource());
    caseRequest.setCoordCode(ingest.getCoordinatorId());
    caseRequest.setContact(setContact(ingest));
    // check this method
    caseRequest.setAddress(setAddress(ingest));
    location.setLat(ingest.getAddress().getLatitude().floatValue());
    location.set_long(ingest.getAddress().getLongitude().floatValue());
    caseRequest.setLocation(location);
    caseRequest.setSpecialInstructions(ingest.getSpecialInstructions());
    // Removed mapping from actionRequest for the ccsQuestionnaireURL;
    // this will be derived from an environment variable and the caseId in the CCS specific mapping
    ccsCaseExtension.setQuestionnaireUrl(ingest.getCcsQuestionnaireURL());
    caseRequest.setCcs(ccsCaseExtension);

    return caseRequest;
  }

  private CCSOutcomeEntity retrieveCache(String caseId) {
    return jobCacheManager.getCachedCCSOutcome(caseId);
  }

  private CCSOutcomeEntity cacheJob() {
    return jobCacheManager.cacheCCSOutcome(new CCSOutcomeEntity("id", "jobJSON"));
  }

  private Contact setContact(CreateFieldWorkerJobRequest ingest) {
    Contact contact = new Contact();
    contact.setName(ingest.getAddress().getPostCode());
    return contact;
  }

  @Override
  public CasePauseRequest convertCancel(CancelFieldWorkerJobRequest cancelIngest) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CaseRequest convertUpdate(UpdateFieldWorkerJobRequest ingest, ModelCase modelCase) {
    throw new UnsupportedOperationException();
  }
}