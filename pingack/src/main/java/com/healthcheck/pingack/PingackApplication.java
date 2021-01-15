package com.healthcheck.pingack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class PingackApplication {

	public static void main(String[] args) { SpringApplication.run(PingackApplication.class, args); }

	// In assenza di PING_TIME, il valore predefinito è 30 secondi
	@Value("${PING_TIME:30}")
	private long pingTime;

	// Lista di host separati da virgole
	@Value("${HOSTS_LIST}")
	private String hostsList;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	// Esegue non appena l'applicazione è pronta
	@EventListener(ApplicationReadyEvent.class)
	public void doRunAfterStartup() {
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(); // Definizione del servizio da schedulare
		Runnable runnable = () -> {
			List<String> hosts = Arrays.asList(hostsList.split("\\s*,\\s*")); // Parsing della lista di host
			for (String hostname : hosts) {
				try {
					RestTemplate restTemplate = new RestTemplate();
					String result = restTemplate.getForObject("http://testing:8080/ping", String.class);
					/*
					* L'URL "http://testing:8080/ping" dovrebbe avere hostname al posto di "testing", nel caso in cui le
					* richieste vengano fatte ai veri container.
					*/
					Gson gson = new Gson();
					JsonObject jsonResponse = gson.fromJson(result, JsonObject.class); // JSON restituito dal microservizio

					if (jsonResponse.get("serviceStatus").getAsString().equals("down") || jsonResponse.get("dbStatus").getAsString().equals("down")) {
						// System.out.println("ananas");
						/*
						 * Struttura del messaggio inviato sul topic logging di Kafka:
						 * {
							 * "service_down" : {
								 * "time": UnixTimeStamp,
								 * "status": JsonResponse,
								 * "service": hostname
							 * }
						 * }
						 */
						long unixTimeStamp = System.currentTimeMillis() / 1000L;
						String kafkaData = "{"
									+ "\"time\" : \"" + unixTimeStamp + "\", "
									+ "\"status\" : \"" + jsonResponse + "\", "
									+ "\"service\" : \"" + hostname + "\""
								+ "}";
						kafkaTemplate.send("logging", "service_down", kafkaData);
					}
					System.out.println(jsonResponse.get("serviceStatus"));
					System.out.println(jsonResponse.get("dbStatus"));

				} catch (ResourceAccessException ex) {
					// Nel caso in cui il servizio non dovesse essere raggiungibile, viene inviato un apposito
					// messaggio sul topic logging di Kafka
					long unixTimeStamp = System.currentTimeMillis() / 1000L;
					String kafkaData = "{"
								+ "\"time\" : \"" + unixTimeStamp + "\", "
								+ "\"status\" : {"
									+ "\"serverUnavailable\": \"Resource could not be accessed.\""
								+"}, "
								+ "\"service\" : \"" + hostname + "\""
							+ "}";
					kafkaTemplate.send("logging", "service_down", kafkaData);
				} catch (Exception ex) {
					// System.out.println("error");
					ex.printStackTrace();
				}
			}
		};
		// Scheduling dell'esecuzione ad intervalli di durata pari a PING_TIME (in secondi)
		service.scheduleAtFixedRate(runnable, 0, pingTime, TimeUnit.SECONDS);
		// System.out.println("pesca scriroppata");
	}
}
