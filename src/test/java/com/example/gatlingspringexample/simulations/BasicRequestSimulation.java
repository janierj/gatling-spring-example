package com.example.gatlingspringexample.simulations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class BasicRequestSimulation extends Simulation {

    private final Logger log = LoggerFactory.getLogger(BasicRequestSimulation.class);

    private final String ENTRIES_API_URL = "https://api.publicapis.org";

    public BasicRequestSimulation() {
        this.setUp(scnEntriesApi.injectOpen(constantUsersPerSec(1).during(Duration.ofSeconds(2))))
                .protocols(httpEntriesProtocol);
    }

    // Protocol Definition
    HttpProtocolBuilder httpEntriesProtocol = http
            .baseUrl(ENTRIES_API_URL)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling Performance Test");

    // Define the scenario
    ScenarioBuilder scnEntriesApi = scenario("Simple load test")
            .exec(http("entries-request")
                    .get("/random")
                    .check(status().is(HttpResponseStatus.OK.code()))
                    .check(bodyString().saveAs("response"))
            ).exec(session -> {
                        ObjectMapper objectMapper = new ObjectMapper();
                        ApiResponse apiResponse = new ApiResponse();
                        try {
                            apiResponse = objectMapper.readValue(session.getString("response"), ApiResponse.class);
                        } catch (JsonProcessingException e) {
                            log.error("Error parsing object", e);
                        }
                        System.out.println(apiResponse);
                        return session;
                    }
            );

    @Data
    public static class ApiResponse {
        private Integer count;
        private List<Entry> entries;

        @Data
        public static class Entry {
            @JsonProperty("API")
            private String api;

            @JsonProperty("Auth")
            private String auth;

            @JsonProperty("Category")
            private String category;

            @JsonProperty("Cors")
            private String cors;

            @JsonProperty("Description")
            private String description;

            @JsonProperty("HTTPS")
            private String https;

            @JsonProperty("Link")
            private String link;
        }
    }
}
