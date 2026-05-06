package com.example.tests;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static com.example.config.TestConfig.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public abstract class BaseTest {
    protected static WireMockServer wireMockServer;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(options()
                .port(MOCK_PORT)
                .bindAddress("0.0.0.0"));
        wireMockServer.start();
        
        WireMock.configureFor(MOCK_HOST, MOCK_PORT);

        RestAssured.config = RestAssured.config()
                .encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset("UTF-8"));

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.URLENC)
                .addHeader("Accept", "application/json")
                .addFilter(new AllureRestAssured())
                .build();
        
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @AfterAll
    static void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}
