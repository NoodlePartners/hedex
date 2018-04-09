package com.noodle.hedex.sakai.rest;

import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class SakaiHedexEvent {

    private String resource;
    private String context;
    private int priority;
    private String event;
    private String sessionId;
    private String userId;
    private boolean modify;
    private String lrsStatement;
    private long eventTime;
}
