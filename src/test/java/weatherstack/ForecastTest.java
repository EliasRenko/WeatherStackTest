package weatherstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weatherstack.models.ForecastErrorSettings;
import weatherstack.models.ForecastSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

public class ForecastTest {

    public static Logger logger = LoggerFactory.getLogger(ForecastTest.class);

    public static ObjectMapper objectMapper = new ObjectMapper();

    public static String url = "http://api.weatherstack.com/current?access_key=%s&query=%s&units=%s&language=%s";

    @ParameterizedTest
    @ValueSource(strings = {"forecastTestSettings.json"})
    @DisplayName("Позитивный тест")
    public void getForecastTest(String resource) throws IOException {

        ForecastSettings[] settings = getProperties(resource);

        for (ForecastSettings s : settings) {

            var fullUrl = String.format(url, s.getAccessKey(), s.getQuery(), s.getUnit(), s.getLanguage(false));
            var result = extract(fullUrl);

            JsonNode root = objectMapper.readTree(result);

            if (root.has("error")) {
                logger.error("Ошибка при запросе.");
                continue;
            }

            JsonNode request = root.get("request");
            JsonNode location = root.get("location");

            assertAll(
                    () -> Assertions.assertEquals(request.path("language").textValue(), s.getLanguage(true), "Параметр `language` не соответствует ожидаемому."),
                    () -> Assertions.assertEquals(request.path("unit").textValue(), s.getUnit(), "Параметр `unit` не соответствует ожидаемому."),
                    () -> Assertions.assertEquals(location.path("name").textValue(), s.getQuery(), "Параметр `name` не соответствует ожидаемому.")
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"forecastTestErrorSettings.json"})
    @DisplayName("Негативный тест")
    public void getForecastErrorsTest(String resource) throws IOException {

        ForecastErrorSettings[] settings = getErrorProperties(resource);

        for (ForecastErrorSettings s : settings) {

            var fullUrl = String.format(url, s.getAccessKey(), s.getQuery(), s.getUnit(), s.getLanguage(false));
            var result = extract(fullUrl);

            JsonNode root = objectMapper.readTree(result);

            if (root.has("error")) {

                JsonNode error = root.get("error");

                var type = error.path("type").textValue();
                var info = error.path("info").textValue();

                String message = "";

                switch (type) {
                    case "404_not_found":
                        message = "Запрос не наиден.";
                        break;
                    case "missing_access_key":
                        message = "Ключ доступа для запроса, не Предоставлен.";
                        break;
                    case "invalid_access_key":
                        message = "Предоставленный ключ доступа некорректный.";
                        break;
                    case "missing_query":
                        message = "Предоставленный параметр `query` некорректный.";
                        break;
                    case "invalid_language":
                        message = "Предоставленный параметр `language` некорректный.";
                        break;
                    case "invalid_unit":
                        message = "Предоставленный параметр `unit` некорректный.";
                        break;
                    case "request_failed":
                        message = "Запрос не удался.";
                        break;
                    case "function_access_restricted":
                        message = "Текущая подписка пользователя не поддерживает эту функцию API.";
                        break;
                }

                Assertions.assertEquals(type, s.getExpectedError());

                logger.info("Ошибка при запросе найдена: " + message + " => " + info);
            }
        }
    }

    private ForecastSettings[] getProperties(String fileName) throws IOException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        return objectMapper.readValue(stream, ForecastSettings[].class);
    }

    private ForecastErrorSettings[] getErrorProperties(String fileName) throws IOException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        return objectMapper.readValue(stream, ForecastErrorSettings[].class);
    }

    private String extract(String url) {
        return given().when().get(url).then().assertThat().statusCode(200).extract().body().asString();
    }
}
