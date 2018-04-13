package org.sakaiproject.hedex.api;

import lombok.Data;

@Data
public class EngagementActivityRecord {

    private final String UNSPECIFIED = "UNSPECIFIED";

    private String personSisId = UNSPECIFIED;
    private String personLmsId = UNSPECIFIED;
    private String sisSectionId = UNSPECIFIED;
    private String lmsSectionId = UNSPECIFIED;
    private String termCode = UNSPECIFIED;
    private String sectionRefNum = UNSPECIFIED;
    private String subjectCode = UNSPECIFIED;
    private String sectionCourseNumber = UNSPECIFIED;
    private String sectionNumber = UNSPECIFIED;
    private String engagementStatus = UNSPECIFIED;
    private long lmsLastAccessDate = 0L;
    private long lmsTotalTime = 0L;
    private int lmsTotalLogin = 0;
}
