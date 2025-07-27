package net.fameless.core.caption;

import org.jetbrains.annotations.Nullable;

public enum Language {
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch");

    private final String identifier;
    private final String friendlyName;

    Language(String identifier, String friendlyName) {
        this.identifier = identifier;
        this.friendlyName = friendlyName;
    }

    public static @Nullable Language ofIdentifier(String identifier) {
        for (Language language : Language.values()) {
            if ((language.identifier.equals(identifier))) {
                return language;
            }
        }
        return null;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getIdentifier() {
        return identifier;
    }
}
