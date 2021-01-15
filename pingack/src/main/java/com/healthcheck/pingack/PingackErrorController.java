package com.healthcheck.pingack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class PingackErrorController implements ErrorController {
    // Si implementa l'intefaccia ErrorController al fine di rimpiazzare il comportamento predefinito

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request) {
        // Gestione personalizzata dell'errore

        String errorURL = request.getRequestURL().toString(); // URL della richiesta
        String sourceIp = request.getHeader("X-FORWARDED-FOR"); // Indirizzo IP
        if (sourceIp == null) {
            sourceIp = request.getRemoteAddr(); // Indirizzo IP
        }
        String method = request.getMethod(); // Metodo
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE); // Status code

        Integer statusCode;
        if (status == null) {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(); // Status code 500
        } else {
            statusCode = Integer.valueOf(status.toString());
            /*
            * Status code 4xx
            * In relazione all'attuale struttura progettuale del microservizio, tale errore non viene mai restituito.
            * Ciò nonostante, si è scelto di inserirlo in vista di eventuali sviluppi futuri.
            */
        }
        long unixTimeStamp = System.currentTimeMillis() / 1000L;
        /*
        * Struttura del messaggio inviato sul topic logging di Kafka:
        * "http_errors" : {
            * "timestamp": UnixTimestamp,
            * "sourceIp": sourceIp,
            * "service": products,
            * "request": path + method,
            * "error": error
        * }
        */
        String kafkaData = "{"
                    + "\"timestamp\" : \"" + unixTimeStamp + "\", "
                    + "\"sourceIp\" : \"" + sourceIp + "\", "
                    + "\"service\" : \"pingack\", "
                    + "\"request\" : \"" + method + " " + errorURL + "\", "
                    + "\"error\" : \"" + statusCode + "\""
                + "}";
        kafkaTemplate.send("logging", "http_errors", kafkaData);
    }

    @Override
    public String getErrorPath() { return null; } // Deprecata ma necessaria

}
