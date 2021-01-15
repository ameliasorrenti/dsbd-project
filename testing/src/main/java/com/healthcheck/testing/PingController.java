package com.healthcheck.testing;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Random;

@Controller
public class PingController {

    @GetMapping(value="/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getPing() {
        String serviceStatus, dbStatus;
        serviceStatus = dbStatus =  "down";
        Random random = new Random();
        float serviceStatusProbability = random.nextFloat();
        float dbStatusProbability = random.nextFloat();
        if (serviceStatusProbability < 0.7) {
            serviceStatus = "up";
        }
        if (dbStatusProbability < 0.7) {
            dbStatus = "up";
        }
        return "{"
                +"\"serviceStatus\" : \""+ serviceStatus +"\", "
                +"\"dbStatus\" : \""+ dbStatus +"\""
                +"}";
    }

}
