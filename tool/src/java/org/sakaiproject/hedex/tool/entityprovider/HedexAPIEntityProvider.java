package org.sakaiproject.hedex.tool.entityprovider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.hedex.api.HedexAssignment;
import org.sakaiproject.hedex.api.EngagementActivityRecord;
import org.sakaiproject.hedex.api.EngagementActivityRecords;
import org.sakaiproject.hedex.api.model.SessionDuration;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentServiceConstants;
import org.sakaiproject.assignment.api.model.Assignment;
import org.sakaiproject.assignment.api.model.AssignmentSubmission;
import org.sakaiproject.assignment.api.model.AssignmentSubmissionSubmitter;
import org.sakaiproject.site.api.SiteService;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class HedexAPIEntityProvider extends AbstractEntityProvider
    implements AutoRegisterEntityProvider, ActionsExecutable {

    private final static String TENANT_ID = "tenantId";
    private final static String REQUESTING_AGENT = "RequestingAgent";
    private final static String TERMS = "terms";
    private final static String START_DATE = "startDate";
    private final static String SEND_CHANGES_ONLY = "sendChangesOnly";
    private final static String LAST_RUN_DATE = "lastRunDate";
    private final static String INCLUDE_ALL_TERM_HISTORY = "lastRunDate";

    private String tenantId;
    private ObjectMapper objectMapper = new ObjectMapper();

    private final static DateFormat startDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private ServerConfigurationService serverConfigurationService;
    private SessionFactory sessionFactory;
    private AssignmentService assignmentService;
    private SecurityService securityService;
    private SiteService siteService;

    public void init() {
        tenantId = serverConfigurationService.getString("hedex.tenantId", "UNSPECIFIED");
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getEntityPrefix() {
		return "hedex-api";
	}

	@EntityCustomAction(action = "Get_Retention_Engagement_EngagementActivity", viewKey = EntityView.VIEW_LIST)
	public ActionReturn getEngagementActivity(EntityReference reference, Map<String, Object> params) {

        String userId = getCheckedUser(reference);
        String requestingAgent = getCheckedRequestingAgent(params, reference);
        final String[] terms = getTerms(params);
        Date startDate = getValidatedDate((String) params.get(START_DATE));
        String sendChangesOnly = (String) params.get(SEND_CHANGES_ONLY);
        String lastRunDate = (String) params.get(LAST_RUN_DATE);
        String includeAllTermHistory = (String) params.get(INCLUDE_ALL_TERM_HISTORY);

        try {
            Session session = sessionFactory.openSession();
            Criteria criteria = session.createCriteria(SessionDuration.class);
            if (startDate != null) {
                criteria.add(Restrictions.ge("startTime", startDate));
            }
            List<SessionDuration> sessionDurations = criteria.list();
            if (sessionDurations.size() > 0) {
                EngagementActivityRecords eaRecords = new EngagementActivityRecords();
                eaRecords.setTenantId(tenantId);
                List<EngagementActivityRecord> engagementActivity = new ArrayList<>();
                Map<String, Long> totalTimes = new HashMap<>();
                Map<String, Integer> totalLogins = new HashMap<>();
                Map<String, Long> lastAccesses = new HashMap<>();
                Map<String, EngagementActivityRecord> records = new HashMap<>();
                for (SessionDuration sd : sessionDurations) {
                    String personLmsId = sd.getUserId();
                    if (!totalTimes.containsKey(personLmsId)) {
                        totalTimes.put(personLmsId, 0L);
                    }
                    Long duration = sd.getDuration();
                    if (duration != null) {
                        totalTimes.put(personLmsId, totalTimes.get(personLmsId) + duration);
                    }
                    if (!totalLogins.containsKey(personLmsId)) {
                        totalLogins.put(personLmsId, 0);
                    }
                    totalLogins.put(personLmsId, totalLogins.get(personLmsId) + 1);
                    if (!lastAccesses.containsKey(personLmsId)) {
                        lastAccesses.put(personLmsId, 0L);
                    }
                    long storedAccessTime = lastAccesses.get(personLmsId);
                    long sessionStartTime = sd.getStartTime().getTime();
                    if (sessionStartTime > storedAccessTime) {
                        lastAccesses.put(personLmsId, sessionStartTime);
                    }
                    if (!records.containsKey(personLmsId)) {
                        EngagementActivityRecord record = new EngagementActivityRecord();
                        record.setPersonLmsId(personLmsId);
                        records.put(personLmsId, record);
                    }
                }

                records.forEach((personLmsId,record) -> {
                    record.setLmsTotalTime(totalTimes.get(personLmsId));
                    record.setLmsTotalLogin(totalLogins.get(personLmsId));
                    record.setLmsLastAccessDate(lastAccesses.get(personLmsId));
                });

                eaRecords.setEngagementActivity(new ArrayList<>(records.values()));
                String json = objectMapper.writeValueAsString(eaRecords);
                return new ActionReturn(Formats.UTF_8, Formats.JSON_MIME_TYPE, json);
            }
        } catch (Exception e) {
            log.error("Failed to get sessions.", e);
        }

        return null;
	}

	@EntityCustomAction(action = "Get_Retention_Engagement_Assignments", viewKey = EntityView.VIEW_LIST)
	public ActionReturn getAssignments(EntityReference reference, Map<String, Object> params) {

        String userId = getCheckedUser(reference);
        String requestingAgent = getCheckedRequestingAgent(params, reference);
        final String[] terms = getTerms(params);
        Date startDate = getValidatedDate((String) params.get(START_DATE));
        String sendChangesOnly = (String) params.get(SEND_CHANGES_ONLY);
        String lastRunDate = (String) params.get(LAST_RUN_DATE);
        String includeAllTermHistory = (String) params.get(INCLUDE_ALL_TERM_HISTORY);

        // Get all the sites in this instance
        List<String> siteIds = siteService.getSiteIds(SiteService.SelectionType.NON_USER, null, null, null, SiteService.SortType.NONE, null);

        SecurityAdvisor advisor = new SecurityAdvisor() {

            public SecurityAdvisor.SecurityAdvice isAllowed(String userId, String function, String reference) {

                if (function.equals(AssignmentServiceConstants.SECURE_ACCESS_ASSIGNMENT)) {
                    return SecurityAdvisor.SecurityAdvice.ALLOWED;
                } else {
                    return SecurityAdvisor.SecurityAdvice.PASS;
                }
            }
        };

        securityService.pushAdvisor(advisor);

        try {
            List<HedexAssignment> hedexAssignments = new ArrayList<>();
            for (String siteId : siteIds) {
                Collection<Assignment> assignmentsForContext = null;
                try {
                    assignmentsForContext = assignmentService.getAssignmentsForContext(siteId);
                } catch (IllegalArgumentException iae) {
                    log.error("Failed to get assignments for siteId " + siteId, iae);
                    continue;
                }
                for (Assignment assignment : assignmentsForContext) {
                    Set<AssignmentSubmission> submissions = (Set<AssignmentSubmission>) assignmentService.getSubmissions(assignment);
                    for (AssignmentSubmission submission : submissions) {
                        for (AssignmentSubmissionSubmitter submitter : submission.getSubmitters()) {
                            HedexAssignment hedexAssignment = new HedexAssignment();
                            hedexAssignment.setAssignmentLmsId(assignment.getId());
                            hedexAssignment.setPersonLmsId(submitter.getSubmitter());
                            hedexAssignments.add(hedexAssignment);
                        }
                    }
                }
            }
            String json = objectMapper.writeValueAsString(hedexAssignments);
            return new ActionReturn(Formats.UTF_8, Formats.JSON_MIME_TYPE, json);
        } catch (Exception e) {
            log.error("Failed to serialise to JSON", e);
        } finally {
            securityService.popAdvisor(advisor);
        }
        return null;
    }

    private String getCheckedUser(EntityReference reference) {

        String userId = developerHelperService.getCurrentUserId();

        if (userId == null) {
            throw new EntityException("You must be logged as the hedex user.", reference.getReference());
        }

        return userId;
    }

    private String getCheckedRequestingAgent(Map<String, Object> params, EntityReference reference) {

        String requestingAgent = (String) params.get(REQUESTING_AGENT);

		if (StringUtils.isBlank(requestingAgent)) {
            throw new EntityException("You must supply a RequestingAgent.", reference.getReference());
        }
        return requestingAgent;
    }

    private Date getValidatedDate(String dateString) {

        Date date = null;
        if (!StringUtils.isBlank(dateString)) {
            try {
                date = startDateFormat.parse(dateString);
            } catch (ParseException pe) {
                log.error("Failed to parse supplied date. The date must be in ISO8601 format.", pe);
            }
        }
        return date;
    }

    private String[] getTerms(Map<String, Object> params) {

        final String termsString = (String) params.get(TERMS);
        return termsString != null ? termsString.split(",") : new String[] {};
    }
}
