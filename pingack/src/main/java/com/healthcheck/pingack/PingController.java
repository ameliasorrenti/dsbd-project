package com.healthcheck.pingack;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PingController {

    @GetMapping(value="/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getPing() {
        return "{"
                    +" \"serviceStatus\" : \"up\","
                    +" \"dbStatus\" : \"up\""
                +"}";
        // La key "dbStatus" è stata introdotta fittiziamente per rendere la
        // risposta compatibile con quella attesa dal microservizio pingack
    }

    @GetMapping(value="/ping-probe", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getPingProbe() {
        return "Alive!";
    }

}
