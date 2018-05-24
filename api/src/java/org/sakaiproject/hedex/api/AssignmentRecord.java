package org.sakaiproject.hedex.api;

import lombok.Data;

@Data
public class AssignmentRecord {

    private final String UNSPECIFIED = "UNSPECIFIED";

    private String personLmsId = UNSPECIFIED;
    private String assignmentLmsId = UNSPECIFIED;
    private String assignType = UNSPECIFIED;
    private String assignTitle = UNSPECIFIED;
    private String assignDueDate = UNSPECIFIED;
    private String assignGrade = UNSPECIFIED;
    private String assignGradeScheme = UNSPECIFIED;
    private String assignScore = UNSPECIFIED;
    private String assignScoreScheme = UNSPECIFIED;
    private String assignHiScore = UNSPECIFIED;
    private String assignLoScore = UNSPECIFIED;
    private String assignFirstAttmpt = UNSPECIFIED;
    private String assignLastAttmpt = UNSPECIFIED;
    private String assignAvgAttmpt = UNSPECIFIED;
    private String assignNumAttempt = UNSPECIFIED;
}
