package org.sakaiproject.hedex.tool.entityprovider;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

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

    @Setter
    private SessionFactory sessionFactory;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getEntityPrefix() {
		return "hedex-api";
	}

	@EntityCustomAction(action = "Get_Retention_Engagement_EngagementActivity", viewKey = EntityView.VIEW_SHOW)
	public ActionReturn getEngagementActivity(EntityReference reference, Map<String, Object> params) {

        String userId = developerHelperService.getCurrentUserId();

        if (userId == null) {
            throw new EntityException("You must be logged in to get the memberships", reference.getReference());
        }

        String tenantId = (String) params.get(TENANT_ID);
        String requestingAgent = (String) params.get(REQUESTING_AGENT);

		if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(requestingAgent)) {
            throw new EntityException("You must supply a tenantId and the RequestingAgent.", reference.getReference());
        }

        String terms = (String) params.get(TERMS);
        String startDate = (String) params.get(START_DATE);
        String sendChangesOnly = (String) params.get(SEND_CHANGES_ONLY);
        String lastRunDate = (String) params.get(LAST_RUN_DATE);
        String includeAllTermHistory = (String) params.get(INCLUDE_ALL_TERM_HISTORY);

        return null;
	}
}
