package weatherstack.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ForecastSettings {

    private String accessKey;
    private String query;
    private String unit;
    private String language;

    public ForecastSettings(@JsonProperty("accessKey") String accessKey, @JsonProperty("query") String query, @JsonProperty("unit") String unit, @JsonProperty("language") String language) {

        this.accessKey = accessKey;
        this.query = query;
        this.unit = unit;
        this.language = language;
    }

    public String getAccessKey() {

        return accessKey;
    }

    public String getQuery() {

        return query;
    }

    public String getUnit() {

        return unit;
    }

    public String getLanguage(Boolean notEmpty) {

        return (language.equals("") && notEmpty ? "en" : language);
    }
}
