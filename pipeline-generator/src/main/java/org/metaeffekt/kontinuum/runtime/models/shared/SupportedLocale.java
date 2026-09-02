package org.metaeffekt.kontinuum.runtime.models.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SupportedLocale {
    EN_US("en_US", "en", "EN"),
    DE_DE("de_DE", "de", "DE");

    final String identifier;
    final String language;
    final String locale;

    SupportedLocale(String identifier, String language, String locale) {
        this.identifier = identifier;
        this.language = language;
        this.locale = locale;
    }

    @JsonValue
    public String getIdentifier() {
        return identifier;
    }

    @JsonCreator
    public static SupportedLocale fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        for (SupportedLocale supportedLocale : values()) {
            if (supportedLocale.name().equalsIgnoreCase(trimmed)
                    || supportedLocale.identifier.equalsIgnoreCase(trimmed)
                    || supportedLocale.language.equalsIgnoreCase(trimmed)
                    || supportedLocale.locale.equalsIgnoreCase(trimmed)) {
                return supportedLocale;
            }
        }
        throw new IllegalArgumentException("Unknown locale: '" + value + "'. Supported values include [en_US, de_DE, en, de, EN_US, DE_DE].");
    }
}
