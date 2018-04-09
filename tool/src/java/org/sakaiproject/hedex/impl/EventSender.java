package org.sakaiproject.hedex.impl;

import java.util.List;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.event.api.Event;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

@Slf4j
public class EventSender {

    private ObjectMapper objectMapper = new ObjectMapper();
    private String hubUrl;
    private String tenantId;

    @Setter
    private ServerConfigurationService serverConfigurationService;

    public void init() {

        tenantId = serverConfigurationService.getString("hedex.tenantId", "unknown");
        hubUrl = serverConfigurationService.getString("hedex.hubUrl", "http://localhost:8080/events");
    }

    public void sendEvents(List<Event> events) {

        log.debug("Sending {} events ...", events.size());
        try {
            String eventsJson = objectMapper.writeValueAsString(new EventsPackage(tenantId, events));
            log.debug("EventsPackage json: {}", eventsJson);
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(hubUrl);
            httpPost.setEntity(new StringEntity(eventsJson, ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpclient.execute(httpPost);
        } catch (Exception e) {
            log.error("Failed to send events to event hub.", e);
        }
    }

    @Getter
    public class EventsPackage {

        private List<Event> events;
        private String tenantId;

        public EventsPackage(String tenantId, List<Event> events) {

            this.tenantId = tenantId;
            this.events = events;
        }
    }
}
