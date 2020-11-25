package com.thehecklers.rsclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@SpringBootApplication
public class RsClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(RsClientApplication.class, args);
    }

}

@Configuration
class ClientConfig {
    @Bean
    RSocketRequester requester(RSocketRequester.Builder builder) {
//        return builder.tcp("localhost", 9091);
        return builder.websocket(URI.create("http://localhost:9091"));
    }
}

@Component
@AllArgsConstructor
class AircraftClient {
    private final RSocketRequester requester;

    //@PostConstruct
    void reqResp() {
        requester.route("reqresp")
                .data(Instant.now())
                .retrieveMono(Aircraft.class)
                .subscribe(ac -> System.out.println("🛩 " + ac));
    }

    //@PostConstruct
    void reqStream() {
        requester.route("reqstream")
                .data(Instant.now())
                .retrieveFlux(Aircraft.class)
                .subscribe(ac -> System.out.println("🛩🛩 " + ac));
    }

    //@PostConstruct
	void fireAndForget() {
    	requester.route("fireforget")
				.data(new Weather(Instant.now(), "SKC, VIS 12SM"))
				.send()
				.subscribe();
	}

	@PostConstruct
    void channel() {
        var obsList = List.of("SKC, VIS 12SM", "OVC 800, VIS 1/2SM", "BKN 8000, VIS 10SM");
        var rnd = new Random();

        requester.route("channel")
                .data(Flux.interval(Duration.ofSeconds(1))
                .map(l -> new Weather(Instant.now(),
                        obsList.get(rnd.nextInt(obsList.size())))))
                .retrieveFlux(Aircraft.class)
                .subscribe(ac -> System.out.println("✈️✈️ " + ac));
    }
}

@EnableScheduling
@Component
@AllArgsConstructor
class FaFComponent {
    private final RSocketRequester requester;

    @Scheduled(fixedRate = 3000)
    void fireAndForget() {
        requester.route("fireforget")
                .data(new Weather(Instant.now(), "SKC, VIS 12SM"))
                .send()
                .subscribe();
    }
}

@Data
@AllArgsConstructor
class Weather {
	private Instant when;
	private String observation;
}

@Data
@AllArgsConstructor
class Aircraft {
    private String callsign, reg, flightno, type;
    private int altitude, heading, speed;
    private double lat, lon;
}