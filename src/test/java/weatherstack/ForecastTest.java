package weatherstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weatherstack.models.ForecastSettings;

import java.io.File;
import java.io.IOException;

import static io.restassured.RestAssured.given;

public class ForecastTest {

    public static Logger logger;

    public static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {

        logger = LoggerFactory.getLogger(ForecastTest.class);

        objectMapper = new ObjectMapper();
    }

    @ParameterizedTest
    @ValueSource(strings = {"forecastTestSettings1.json", "forecastTestSettings2.json"})
    public void getForecastTest(String resource) throws IOException {

        File file = new File(this.getClass().getClassLoader().getResource(resource).getFile());

        var settings = objectMapper.readValue(file, ForecastSettings[].class);

        for (ForecastSettings s : settings) {

            var url = String.format("http://api.weatherstack.com/current?access_key=%s&query=%s&units=%s&language=%s", s.getAccessKey(), s.getQuery(), s.getUnit(), s.getLanguage(false));

            var result = extract(url);

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
                        message = "Ключ доступа для запроса, не предоставлин.";
                        break;
                    case "invalid_access_key":
                        message = "Предоставлин ключ доступа некорректный.";
                        break;
                    case "missing_query":
                        message = "Предоставлин параметр `query` некорректный.";
                        break;
                    case "invalid_language":
                        message = "Предоставлин параметр `language` некорректный.";
                        break;
                    case "invalid_unit":
                        message = "Предоставлин параметр `unit` некорректный.";
                        break;
                }

                Assertions.fail("Ошибка при запросе: " + message + " => " + info);

                continue;
            }

            JsonNode request = root.get("request");
            JsonNode location = root.get("location");

            Assertions.assertAll(
                    () -> Assertions.assertEquals(request.path("language").textValue(), s.getLanguage(true), "Предоставлин параметр `language` не соответствует ожидаемого."),
                    () -> Assertions.assertEquals(request.path("unit").textValue(), s.getUnit(), "Предоставлин параметр `unit` не соответствует ожидаемого."),
                    () -> Assertions.assertEquals(location.path("name").textValue(), s.getQuery(), "Предоставлин параметр `name` не соответствует ожидаемого.")
            );
        }
    }

    private String extract(String url) {
        return given().when().get(url).then().assertThat().statusCode(200).extract().body().asString();
    }
}
