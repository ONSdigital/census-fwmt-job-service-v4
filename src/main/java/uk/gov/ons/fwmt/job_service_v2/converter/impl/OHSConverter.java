package uk.gov.ons.fwmt.job_service_v2.converter.impl;

import com.consiliumtechnologies.schemas.mobile._2009._03.visitstypes.AdditionalPropertyCollectionType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisemessages.CreateJobRequest;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.AddressDetailType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.ContactInfoType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.JobIdentityType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.JobType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.LocationType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.ObjectFactory;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.ResourceIdentityType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.SkillCollectionType;
import com.consiliumtechnologies.schemas.mobile._2015._05.optimisetypes.WorldIdentityType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.fwmt.fwmtgatewaycommon.data.FWMTCreateJobRequest;
import uk.gov.ons.fwmt.job_service_v2.converter.TMConverter;
import uk.gov.ons.fwmt.job_service_v2.utils.CreateJobBuilder;
import uk.gov.ons.fwmt.job_service_v2.utils.TMJobConverter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.List;

import static uk.gov.ons.fwmt.job_service_v2.utils.TMJobConverter.addAdditionalProperty;
import static uk.gov.ons.fwmt.job_service_v2.utils.TMJobConverter.addAddressLines;
import static uk.gov.ons.fwmt.job_service_v2.utils.TMJobConverter.checkNumberOfAddressLines;

@Component("LMS")
public class OHSConverter implements TMConverter {

  private static final String DESCRIPTION = "OHS";
  private static final String SKILL = "OHS";
  private static final String WORK_TYPE = "OHS";
  private static final String DEFAULT_WORLD = "Default";
  private static final String DEFAULT_WAVE = "1";
  private static final String ADDITIONAL_PROPERTY_WAVE = "wave";
  private static final String ADDITIONAL_PROPERTY_TLA = "TLA";
  private static final String DEFAULT_TLA = "OHS";

  @Value("${totalmobile.modworld}")
  private String modWorld;

  @Value("${fwmt.workTypes.ohs.duration}")
  private int duration;

  private DatatypeFactory datatypeFactory;
  private ObjectFactory objectFactory;

  public OHSConverter() throws DatatypeConfigurationException {
    datatypeFactory = DatatypeFactory.newInstance();
    objectFactory = new ObjectFactory();
  }

  public CreateJobRequest convert(FWMTCreateJobRequest ingest) {
    CreateJobBuilder builder = new CreateJobBuilder(datatypeFactory)
        .withDescription(DESCRIPTION)
        .withWorkType(WORK_TYPE)
        .withDescription("Census - " + ingest.getAddress().getPostCode())
        .withDuration(duration)
        .withWorld(modWorld)
        .withVisitComplete(false)
        .withEmergency(false)
        .withDispatched(false)
        .withAppointmentPending(false)
        .addSkill(SKILL)
        .withIdentity(ingest.getJobIdentity())
        .withDueDate(ingest.getDueDate().atTime(23, 59, 59).atZone(ZoneId.of("UTC")))
        .withContactName(ingest.getAddress().getPostCode())
        .withAddressLines(
            ingest.getAddress().getLine1(),
            ingest.getAddress().getLine2(),
            ingest.getAddress().getLine3(),
            ingest.getAddress().getLine4(),
            ingest.getAddress().getTownName()
        )
        .withPostCode(ingest.getAddress().getPostCode())
        .withGeoCoords(ingest.getAddress().getLongitude(), ingest.getAddress().getLatitude())
        .withAdditionalProperties(ingest.getAdditionalProperties())
        .withAdditionalProperty(ADDITIONAL_PROPERTY_WAVE, DEFAULT_WAVE)
        .withAdditionalProperty(ADDITIONAL_PROPERTY_TLA, DEFAULT_TLA);

    if (ingest.isPreallocatedJob()) {
      builder.withWorld(DEFAULT_WORLD);
      builder.withAllocatedUser("test");
    }

    return builder.build();

    // TODO resolve this section
//    job.setWorld(new WorldIdentityType());
//    if (ingest.isPreallocatedJob()) {
//      job.getWorld().setReference(DEFAULT_WORLD);
//      if (StringUtils.isNotBlank(ingest.getMandatoryResourceAuthNo())) {
//        job.setAllocatedTo(new ResourceIdentityType());
//        job.getAllocatedTo().setUsername("test"); //lookup not defined yet
//      }
//    } else {
//      job.getWorld().setReference(MOD_WORLD);
//      if (StringUtils.isNotBlank(ingest.getMandatoryResourceAuthNo())) {
//        job.setMandatoryResource(new ResourceIdentityType());
//        job.getMandatoryResource().setUsername("temp");
//      }
//    }
  }
}