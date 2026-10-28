package org.metaeffekt.kontinuum.runtime.models.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of asset delivered by a url resolver. Determines which extraction processor is used.
 */
public enum AssetType {
    DEFAULT("default"),
    WINDOWS("windows"),
    LINUX("linux");

    private final String identifier;

    AssetType(String identifier) {
        this.identifier = identifier;
    }

    @JsonValue
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return identifier;
    }

    @JsonCreator
    public static AssetType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT;
        }
        String trimmed = value.trim();
        for (AssetType assetType : values()) {
            if (assetType.identifier.equalsIgnoreCase(trimmed)) {
                return assetType;
            }
        }
        throw new IllegalArgumentException(
                "Unknown assetType: '" + value + "'. Supported values are [default, windows, linux].");
    }
}