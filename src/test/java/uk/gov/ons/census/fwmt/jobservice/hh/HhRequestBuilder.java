package uk.gov.ons.census.fwmt.jobservice.hh;

import uk.gov.ons.census.fwmt.common.rm.dto.ActionInstructionType;
import uk.gov.ons.census.fwmt.common.rm.dto.FwmtActionInstruction;

public final class HhRequestBuilder {

    public static FwmtActionInstruction createPauseInstruction() {
        return FwmtActionInstruction.builder()
                .actionInstruction(ActionInstructionType.PAUSE)
                .surveyName("CENSUS")
                .addressType("HH")
                .addressLevel("U")
                .caseId("ac623e62-4f4b-11eb-ae93-0242ac130002")
                .pauseFrom("2020")
                .build();
    }
}