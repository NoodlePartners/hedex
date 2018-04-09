package com.noodle.hedex.sakai.rest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
public class IncomingEventDataController {

    @RequestMapping(value = "/events", method = RequestMethod.POST)
    public void storeEvents(@RequestBody EventsPackage events) {
        System.out.println("Tenant ID: " + events.getTenantId());
    }
}
