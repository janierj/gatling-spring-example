package com.example.gatlingspringexample.simulations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ChainRequestsSimulation extends Simulation {

    private final Logger log = LoggerFactory.getLogger(ChainRequestsSimulation.class);

    private final String POST_API_URL = "https://httpbin.org";
    private final String GENDERIZE_API_URL = "https://api.genderize.io/?name=";
    private ObjectMapper objectMapper;

    public ChainRequestsSimulation() {
        this.objectMapper = new ObjectMapper();
        this.setUp(scnEntriesApi.injectClosed(constantConcurrentUsers(2).during(Duration.ofSeconds(5))))
                .protocols(httpProtocol);
    }

    private List<Map<String, Object>> createUsernames() {
        Faker faker = new Faker();
        List<Map<String, Object>> usernames = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            usernames.add(Map.of("username", faker.name().firstName()));
        }
        System.out.println(usernames);
        return usernames;
    }

    // Protocol Definition
    HttpProtocolBuilder httpProtocol = http
            .baseUrl(POST_API_URL)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling Performance Test");

    // Define the scenario
    ScenarioBuilder scnEntriesApi = scenario("Load test using chain requests")
            .feed(listFeeder(createUsernames()).random())
            .exec(http("post-users-request")
                    .post("/post")
                    .header("Content-Type", "application/json")
                    .body(StringBody("{ \"username\": \"#{username}\" }"))
                    .check(status().is(HttpResponseStatus.OK.code()))
                    .check(bodyString().saveAs("response"))                 // Save the response body in the session
            ).exec(http("genderize-user-request")                     // This is a chain request using the response of the previous request
                    .get(session -> {                                       // Use the session stored data
                        PostApiResponse postApiResponse = new PostApiResponse();
                        try {
                            postApiResponse = objectMapper.readValue(session.getString("response"), PostApiResponse.class); // Map the response in an object
                        } catch (JsonProcessingException e) {
                            log.error("Error parsing object", e);
                        }
                        System.out.println(postApiResponse);
                        return GENDERIZE_API_URL + postApiResponse.getJson().get("username");
                    })
                    .check(status().is(HttpResponseStatus.OK.code()))
                    .check(bodyString().saveAs("response")) // Save the response body in the session
            ).exec(session -> {
                GenderizeApiResponse genderizeApiResponse = new GenderizeApiResponse();
                try {
                    genderizeApiResponse = objectMapper.readValue(session.getString("response"), GenderizeApiResponse.class);
                } catch (JsonProcessingException e) {
                    log.error("Error parsing object", e);
                }

                System.out.println(genderizeApiResponse);
                return session;
            });

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostApiResponse {
        private String data;
        private Map<String, String> json;
        private String url;
        private String origin;
        private Header headers;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Header {
            @JsonProperty("Accept")
            private String accept;

            @JsonProperty("Content-Length")
            private String contentLength;

            @JsonProperty("Content-Type")
            private String contentType;

            @JsonProperty("Host")
            private String host;

            @JsonProperty("User-Agent")
            private String userAgent;

            @JsonProperty("X-Amzn-Trace-Id")
            private String xAmznTraceId;
        }
    }

    @Data
    public static class GenderizeApiResponse {
        private String name;
        private String gender;
        private float probability;
        private int count;
    }
}
