package org.sakaiproject.hedex.tool.entityprovider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

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
import org.sakaiproject.hedex.api.EngagementActivityRecord;
import org.sakaiproject.hedex.api.EngagementActivityRecords;
import org.sakaiproject.hedex.api.model.SessionDuration;

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

    private ServerConfigurationService serverConfigurationService;
    private SessionFactory sessionFactory;

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

        String userId = developerHelperService.getCurrentUserId();

        if (userId == null) {
            throw new EntityException("You must be logged as the hedex user to get the engagement activity.", reference.getReference());
        }

        String requestingAgent = (String) params.get(REQUESTING_AGENT);

		/*if (StringUtils.isBlank(requestingAgent)) {
            throw new EntityException("You must supply a tenantId and the RequestingAgent.", reference.getReference());
        }*/

        String terms = (String) params.get(TERMS);
        String startDate = (String) params.get(START_DATE);
        String sendChangesOnly = (String) params.get(SEND_CHANGES_ONLY);
        String lastRunDate = (String) params.get(LAST_RUN_DATE);
        String includeAllTermHistory = (String) params.get(INCLUDE_ALL_TERM_HISTORY);

        try {
            Session session = sessionFactory.openSession();
            List<SessionDuration> sessionDurations = session.createCriteria(SessionDuration.class).list();
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
}
