package org.sakaiproject.hedex.api;

import lombok.Data;

@Data
public class AttendanceRecord {

    private String personSisId;
    private String personLmsId;
    private String sisSectionId;
    private String lmsSectionId;
    private String termCode;
    private String sectionRefNum;
    private String subjectCode;
    private String sectionCourseNumber;
    private String sectionNumber;
    private String attendanceStatus;
    private String totalAttendanceEvents;
    private String totalAbsentEvents;
    private String totalTardyEvents;
}
