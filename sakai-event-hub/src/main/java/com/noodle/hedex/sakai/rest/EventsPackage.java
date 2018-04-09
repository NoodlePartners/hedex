package com.noodle.hedex.sakai.rest;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class EventsPackage {

    private List<SakaiHedexEvent> events;
    private String tenantId;
}
