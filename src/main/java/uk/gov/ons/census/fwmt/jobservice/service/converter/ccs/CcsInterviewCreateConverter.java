package uk.gov.ons.census.fwmt.jobservice.service.converter.ccs;

import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.fwmt.common.data.tm.Address;
import uk.gov.ons.census.fwmt.common.data.tm.CaseRequest;
import uk.gov.ons.census.fwmt.common.data.tm.CaseType;
import uk.gov.ons.census.fwmt.common.data.tm.CcsCaseExtension;
import uk.gov.ons.census.fwmt.common.data.tm.Contact;
import uk.gov.ons.census.fwmt.common.data.tm.Geography;
import uk.gov.ons.census.fwmt.common.data.tm.SurveyType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;
import uk.gov.ons.census.fwmt.jobservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.jobservice.service.converter.common.CommonCreateConverter;

import java.util.List;
import java.util.Objects;

public class CcsInterviewCreateConverter  {

  public static String eqUrl;

  private CcsInterviewCreateConverter(@Value("${eq.url}") String eqUrl) {
    CcsInterviewCreateConverter.eqUrl = eqUrl;
  }

  public static CaseRequest.CaseRequestBuilder convertCcs(
      FwmtActionInstruction ffu, GatewayCache cache, CaseRequest.CaseRequestBuilder builder) {
    CaseRequest.CaseRequestBuilder commonBuilder = CommonCreateConverter.convertCommon(ffu, cache, builder);

    commonBuilder.type(CaseType.valueOf(ffu.getAddressType()));
    commonBuilder.surveyType(SurveyType.CCS_INT);
    commonBuilder.category(ffu.getAddressType().equals("HH") ? "HH" : "CE");

    commonBuilder.estabType(ffu.getEstabType());
    commonBuilder.coordCode(ffu.getFieldCoordinatorId());

    Contact outContact = Contact.builder()
        .organisationName(ffu.getOrganisationName() != null ? ffu.getOrganisationName() : "")
        .name((cache != null ? ffu.getOrganisationName() : "")
            + "" + (ffu.getOrganisationName() != null ? ffu.getOrganisationName() : "")
            + "" + (ffu.getOrganisationName() != null ? ffu.getOrganisationName() : ""))
        .build();

    commonBuilder.contact(outContact);

    Geography outGeography = Geography.builder().oa(ffu.getOa()).build();

    Address outAddress = Address.builder()
        .lines(List.of(
            ffu.getAddressLine1(),
            Objects.toString(ffu.getAddressLine2(), ""),
            Objects.toString(ffu.getAddressLine3(), "")
        ))
        .town(ffu.getTownName())
        .postcode(ffu.getPostcode())
        .geography(outGeography)
        .build();
    commonBuilder.address(outAddress);

    return commonBuilder;
  }

  public static CaseRequest convertCcsInterview(FwmtActionInstruction ffu, GatewayCache cache) {
    return CcsInterviewCreateConverter
        .convertCcs(ffu, cache, CaseRequest.builder())
        .ccs(CcsCaseExtension.builder().questionnaireUrl(eqUrl).build())
        .specialInstructions(((cache != null && cache.getAccessInfo() != null && !cache.getAccessInfo().isEmpty()) ?
            cache.getAccessInfo()
                + "\n" : ""))
        .description(
            ((cache != null && cache.getCareCodes() != null && !cache.getCareCodes().isEmpty()) ? cache.getCareCodes()
                + "\n" : ""))
        .build();
  }
}