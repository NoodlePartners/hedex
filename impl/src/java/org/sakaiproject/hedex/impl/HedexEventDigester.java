package org.sakaiproject.hedex.impl;

import java.util.Arrays;
/*import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;*/
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentServiceConstants;
import org.sakaiproject.assignment.api.AssignmentReferenceReckoner;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.assignment.api.model.AssignmentSubmission;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.presence.api.PresenceService;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import org.sakaiproject.hedex.api.model.AssignmentSubmissions;
import org.sakaiproject.hedex.api.model.CourseVisits;
import org.sakaiproject.hedex.api.model.SessionDuration;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HedexEventDigester implements Observer {

    private final String PRESENCE_SUFFIX = "-presence";

    private static final List<String> HANDLED_EVENTS
        = Arrays.asList(UsageSessionService.EVENT_LOGIN
                        , UsageSessionService.EVENT_LOGOUT
                        , AssignmentConstants.EVENT_GRADE_ASSIGNMENT_SUBMISSION
                        , AssignmentConstants.EVENT_SUBMIT_ASSIGNMENT_SUBMISSION
                        , PresenceService.EVENT_PRESENCE);

    @Setter
    private EventTrackingService eventTrackingService;

    @Setter
    private SecurityService securityService;

    @Setter
    private ServerConfigurationService serverConfigurationService;

    @Setter
    private SessionFactory sessionFactory;

    @Setter
    private AssignmentService assignmentService;

    public void init() {

        log.debug("init()");

        if (serverConfigurationService.getBoolean("hedex.digester.enabled", true)) {
            eventTrackingService.addObserver(this);
        } else {
            log.info("HEDEX event digester not enabled on this server");
        }
    }

    public void update(Observable o, final Object arg) {

        if (arg instanceof Event) {
            Event event = (Event) arg;
            String eventName = event.getEvent();
            log.debug("Event '{}' ...", eventName);
            String eventUserId = event.getUserId();
            if (HANDLED_EVENTS.contains(eventName)  && !EventTrackingService.UNKNOWN_USER.equals(eventUserId)) {

                log.debug("Handling event '{}' ...", eventName);

                final String sessionId = event.getSessionId();
                final String reference = event.getResource();

                if (UsageSessionService.EVENT_LOGIN.equals(eventName)) {
                    try {
                        SessionDuration sd = new SessionDuration();
                        sd.setUserId(eventUserId);
                        sd.setSessionId(sessionId);
                        sd.setStartTime(event.getEventTime());
                        Session session = sessionFactory.openSession();
                        Transaction tx = session.beginTransaction();
                        session.save(sd);
                        tx.commit();
                    } catch (Exception e) {
                        log.error("Failed to in insert new SessionDuration", e);
                    }
                } else if (UsageSessionService.EVENT_LOGOUT.equals(eventName)) {
                    try {
                        Session session = sessionFactory.openSession();
                        List<SessionDuration> sessionDurations = session.createCriteria(SessionDuration.class)
                            .add(Restrictions.eq("sessionId", sessionId)).list();
                        if (sessionDurations.size() == 1) {
                            SessionDuration sd = sessionDurations.get(0);
                            sd.setDuration(event.getEventTime().getTime() - sd.getStartTime().getTime());
                            Transaction tx = session.beginTransaction();
                            session.update(sd);
                            tx.commit();
                        } else {
                            log.error("No SessionDuration for event sessionId: " + sessionId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to in insert new SessionDuration", e);
                    }
                } else if (AssignmentConstants.EVENT_SUBMIT_ASSIGNMENT_SUBMISSION.equals(eventName)) {
                    // We need to check for the fully formed submit event.
                    if (reference.contains("/")) {
                        AssignmentReferenceReckoner.AssignmentReference submissionReference
                            = AssignmentReferenceReckoner.reckoner().reference(reference).reckon();
                        String siteId = event.getContext();
                        String assignmentId = submissionReference.getContainer();
                        String submissionId = submissionReference.getId();
                        try {
                            Assignment assignment = assignmentService.getAssignment(assignmentId);

                            Assignment.GradeType gradeType = assignment.getTypeOfGrade();
                            // Lookup the current AssignmentSubmissions record. There should only
                            // be <= 1 for this user and assignment.
                            Session session = sessionFactory.getCurrentSession();
                            List<AssignmentSubmissions> assignmentSubmissionss
                                = session.createCriteria(AssignmentSubmissions.class)
                                    .add(Restrictions.eq("userId", eventUserId))
                                    .add(Restrictions.eq("assignmentId", assignmentId))
                                    .add(Restrictions.eq("submissionId", submissionId)).list();

                            assert assignmentSubmissionss.size() <= 1;

                            if (assignmentSubmissionss.size() <= 0) {
                                // No record yet. Create one.
                                AssignmentSubmissions as = new AssignmentSubmissions();
                                as.setUserId(eventUserId);
                                as.setAssignmentId(assignmentId);
                                as.setSubmissionId(submissionId);
                                as.setSiteId(siteId);
                                as.setTitle(assignment.getTitle());
                                as.setDueDate(Date.from(assignment.getDueDate()));
                                as.setNumSubmissions(1);

                                session.persist(as);
                            } else {
                                AssignmentSubmissions as = assignmentSubmissionss.get(0);
                                as.setNumSubmissions(as.getNumSubmissions() + 1);
                                session.update(as);
                            }
                        } catch (Exception e) {
                            log.error("Failed to in insert/update AssignmentSubmissions", e);
                        }
                    }
                } else if (AssignmentConstants.EVENT_GRADE_ASSIGNMENT_SUBMISSION.equals(eventName)) {
                    AssignmentReferenceReckoner.AssignmentReference submissionReference
                        = AssignmentReferenceReckoner.reckoner().reference(reference).reckon();

                    final String siteId = submissionReference.getContext();
                    final String assignmentId = submissionReference.getContainer();
                    final String submissionId = submissionReference.getId();

                    SecurityAdvisor sa = unlock(new String[] {AssignmentServiceConstants.SECURE_ACCESS_ASSIGNMENT_SUBMISSION
                                                    , AssignmentServiceConstants.SECURE_ACCESS_ASSIGNMENT
                                                    , AssignmentServiceConstants.SECURE_ADD_ASSIGNMENT_SUBMISSION});

                    try {
                        Session session = sessionFactory.getCurrentSession();
                        log.debug("Searching for record for assignment id {} and submission id {}"
                                    , assignmentId, submissionId);
                        final List<AssignmentSubmissions> assignmentSubmissionss
                            = session.createCriteria(AssignmentSubmissions.class)
                                .add(Restrictions.eq("assignmentId", assignmentId))
                                .add(Restrictions.eq("submissionId", submissionId)).list();

                        assert assignmentSubmissionss.size() == 1;

                        final Assignment assignment = assignmentService.getAssignment(assignmentId);
                        assert assignment != null;
                        final Assignment.GradeType gradeType = assignment.getTypeOfGrade();

                        if (assignmentSubmissionss.size() == 1) {
                            log.debug("One HEDEX submissions record found.");
                            final AssignmentSubmission submission = assignmentService.getSubmission(submissionId);
                            assert submission != null;
                            final String grade = submission.getGrade();
                            log.debug("GRADE: {}", grade);
                            assert grade != null;
                            if (grade != null) {
                                AssignmentSubmissions as = assignmentSubmissionss.get(0);
                                if (as.getFirstScore() == null) {
                                    log.debug("This is the first grading");
                                    // First time this submission has been graded
                                    as.setFirstScore(grade);
                                    as.setLastScore(grade);
                                    if (gradeType.equals(Assignment.GradeType.SCORE_GRADE_TYPE)) {
                                        // This is a numeric grade, so we can do numeric stuff with it.
                                        try {
                                            int numericScore = Integer.parseInt(grade);
                                            as.setLowestScore(numericScore);
                                            as.setHighestScore(numericScore);
                                            as.setAverageScore((float)numericScore);
                                        } catch (NumberFormatException nfe) {
                                            log.error("Failed to set scores on graded submission "
                                                        + submissionId + " - NumberFormatException on " + grade);
                                        }
                                    }
                                } else {
                                    log.debug("This is not the first grading");
                                    as.setLastScore(grade);
                                    if (gradeType.equals(Assignment.GradeType.SCORE_GRADE_TYPE)) {
                                        // This is a numeric grade, so we can do numeric stuff with it.
                                        try {
                                            int numericScore = Integer.parseInt(grade);
                                            if (numericScore < as.getLowestScore()) as.setLowestScore(numericScore);
                                            else if (numericScore > as.getHighestScore()) as.setHighestScore(numericScore);
                                            as.setAverageScore((float)((as.getLowestScore() + as.getHighestScore()) / 2));
                                        } catch (NumberFormatException nfe) {
                                            log.error("Failed to set scores on graded submission "
                                                        + submissionId + " - NumberFormatException on " + grade);
                                        }
                                    }
                                }
                                session.update(as);
                            } else {
                                log.error("Null grade set on submission " + submissionId
                                        + ". This is not right. We've had the event, we should have the grade.");
                            }
                        } else {
                            log.error("No submission for id: " + submissionId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to in insert/update AssignmentSubmissions", e);
                    } finally {
                        securityService.popAdvisor(sa);
                    }
                } else if (PresenceService.EVENT_PRESENCE.equals(eventName)) {
                    // Parse out the course id
                    String compoundId = reference.substring(reference.lastIndexOf("/") + 1);
                    String siteId = compoundId.substring(0, compoundId.indexOf(PRESENCE_SUFFIX));

                    if (siteId.startsWith("~")) {
                        // This is a user workspace
                        return;
                    }

                    Session session = sessionFactory.openSession();

                    List<CourseVisits> courseVisitss = session.createCriteria(CourseVisits.class)
                        .add(Restrictions.eq("userId", eventUserId))
                        .add(Restrictions.eq("siteId", siteId)).list();

                    CourseVisits courseVisits = null;

                    assert courseVisitss.size() <= 1;

                    if (courseVisitss.size() <= 0) {
                        courseVisits = new CourseVisits();
                        courseVisits.setUserId(eventUserId);
                        courseVisits.setSiteId(siteId);
                        courseVisits.setNumVisits(1L);
                    } else {
                        courseVisits = courseVisitss.get(0);
                        courseVisits.setNumVisits(courseVisits.getNumVisits() + 1L);
                    }
                    courseVisits.setLatestVisit(event.getEventTime());
                    Transaction tx = session.beginTransaction();
                    session.saveOrUpdate(courseVisits);
                    tx.commit();
                }
            }
        }
    }

	/**
     * Supply null to this and everything will be allowed. Supply
     * a list of functions and only they will be allowed.
     */
    private SecurityAdvisor unlock(final String[] functions) {

        SecurityAdvisor securityAdvisor = new SecurityAdvisor() {
                public SecurityAdvice isAllowed(String userId, String function, String reference) {

                    if (functions != null) {
                        if (Arrays.asList(functions).contains(function)) {
                            return SecurityAdvice.ALLOWED;
                        } else {
                            return SecurityAdvice.NOT_ALLOWED;
                        }
                    } else {
                        return SecurityAdvice.ALLOWED;
                    }
                }
            };
        securityService.pushAdvisor(securityAdvisor);
        return securityAdvisor;
    }
}
