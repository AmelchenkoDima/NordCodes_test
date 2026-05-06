package com.example.api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import static com.example.config.TestConfig.*;
import static io.restassured.RestAssured.given;

public class AppClient {

    @Step("Отправка запроса action={action} с токеном {token}")
    public Response sendAction(String token, String action) {
        return given()
                .header("X-Api-Key", API_KEY)
                .formParam("token", token)
                .formParam("action", action)
                .post(ENDPOINT);
    }

    @Step("Отправка запроса action={action} без API-ключа")
    public Response sendActionWithoutKey(String token, String action) {
        return given()
                .formParam("token", token)
                .formParam("action", action)
                .post(ENDPOINT);
    }
}
