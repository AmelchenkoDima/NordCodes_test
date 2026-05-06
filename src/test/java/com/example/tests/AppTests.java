package com.example.tests;

import com.example.api.AppClient;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.is;

@Epic("Тестирование API веб-сервиса")
@Feature("Управление сессиями и действиями пользователей")
public class AppTests extends BaseTest {
    private final AppClient client = new AppClient();

    @BeforeEach
    void resetMocks() {
        wireMockServer.resetAll();
        
        wireMockServer.stubFor(any(anyUrl())
                .atPriority(10)
                .willReturn(okJson("{\"result\":\"OK\", \"status\":\"success\", \"message\":\"Processed\"}")));

        wireMockServer.stubFor(post(urlMatching(".*auth.*"))
                .atPriority(5)
                .willReturn(okJson("{\"result\":\"OK\", \"status\":\"success\", \"message\":\"Login successful\", \"token\":\"" + UUID.randomUUID() + "\"}")));

        wireMockServer.stubFor(post(urlMatching(".*doAction.*"))
                .atPriority(5)
                .willReturn(okJson("{\"result\":\"OK\", \"status\":\"success\", \"message\":\"Action performed\"}")));
    }

    private String generateValidToken() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    @Test
    @Story("Аутентификация (LOGIN)")
    @DisplayName("Успешный вход пользователя")
    @Description("Проверка авторизации при корректном ответе внешнего сервиса.")
    void testSuccessfulLogin() {
        String token = generateValidToken();

        client.sendAction(token, "LOGIN")
                .then()
                .statusCode(200)
                .body("result", is("OK"));

        wireMockServer.verify(postRequestedFor(urlMatching(".*auth.*"))
                .withRequestBody(containing(token)));
    }

    @Test
    @Story("Выполнение действий (ACTION)")
    @DisplayName("Успешное выполнение действия после LOGIN")
    @Description("Проверка, что авторизованный пользователь может выполнять ACTION.")
    void testSuccessfulAction() {
        String token = generateValidToken();
        
        client.sendAction(token, "LOGIN").then().statusCode(200);

        client.sendAction(token, "ACTION")
                .then()
                .statusCode(200)
                .body("result", is("OK"));

        wireMockServer.verify(postRequestedFor(urlMatching(".*doAction.*"))
                .withRequestBody(containing(token)));
    }

    @Test
    @Story("Безопасность")
    @DisplayName("Отказ в ACTION без предварительного LOGIN")
    void testActionWithoutLogin() {
        client.sendAction(generateValidToken(), "ACTION")
                .then()
                .statusCode(403)
                .body("result", is("ERROR"));
    }

    @Test
    @Story("Завершение сессии (LOGOUT)")
    @DisplayName("Успешный выход из системы (LOGOUT)")
    void testLogoutSuccess() {
        String token = generateValidToken();
        
        client.sendAction(token, "LOGIN").then().statusCode(200);

        client.sendAction(token, "LOGOUT")
                .then()
                .statusCode(200)
                .body("result", is("OK"));

        client.sendAction(token, "ACTION")
                .then()
                .statusCode(403);
    }

    @Test
    @Story("Безопасность")
    @DisplayName("Ошибка при отсутствии API-ключа")
    void testMissingApiKey() {
        client.sendActionWithoutKey(generateValidToken(), "LOGIN")
                .then()
                .statusCode(401);
    }

    @Test
    @Story("Валидация")
    @DisplayName("Ошибка при неверной длине токена")
    void testTokenTooShort() {
        client.sendAction("INVALID_TOKEN", "LOGIN")
                .then()
                .statusCode(400)
                .body("result", is("ERROR"));
    }

    @ParameterizedTest(name = "Токен: {0}")
    @ValueSource(strings = {"!!!BAD_CHARS!!!", "lowercase_token_12345", "  SPACES  "})
    @Story("Валидация")
    @DisplayName("Ошибка при недопустимых символах в токене")
    void testInvalidTokenCharacters(String invalidToken) {
        client.sendAction(invalidToken, "LOGIN")
                .then()
                .statusCode(400)
                .body("result", is("ERROR"));
    }

    @Test
    @Story("Отказоустойчивость")
    @DisplayName("Ошибка приложения при 500 от внешнего сервиса")
    void testExternalAuthError() {
        String token = generateValidToken();
        wireMockServer.stubFor(post(urlMatching(".*auth.*"))
                .atPriority(1)
                .willReturn(serverError()));

        client.sendAction(token, "LOGIN")
                .then()
                .statusCode(500)
                .body("result", is("ERROR"));
    }

    @Test
    @Story("Валидация")
    @DisplayName("Ошибка при неизвестном типе действия")
    void testUnknownAction() {
        client.sendAction(generateValidToken(), "UNKNOWN_ACTION")
                .then()
                .statusCode(400);
    }
}
