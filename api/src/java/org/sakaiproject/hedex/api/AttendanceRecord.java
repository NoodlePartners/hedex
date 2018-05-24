package org.sakaiproject.hedex.api;

import lombok.Data;

@Data
public class AttendanceRecord {

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
    private String attendanceStatus = UNSPECIFIED;
    private String totalAttendanceEvents = UNSPECIFIED;
    private String totalAbsentEvents = UNSPECIFIED;
    private String totalTardyEvents = UNSPECIFIED;
}
