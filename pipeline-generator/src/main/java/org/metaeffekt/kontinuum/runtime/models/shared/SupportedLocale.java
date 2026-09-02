package org.metaeffekt.kontinuum.runtime.models.shared;

import lombok.Getter;

@Getter
public enum SupportedLocale {
    EN_US("en_US", "en", "EN"),
    DE_DE("de_DE", "de", "DE");

    final String identifier;
    final String language;
    final String locale;

    SupportedLocale(String identifier, String  language, String locale) {
        this.identifier = identifier;
        this.language = language;
        this.locale = locale;
    }
}
