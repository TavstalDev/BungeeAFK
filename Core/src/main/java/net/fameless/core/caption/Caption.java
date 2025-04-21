package net.fameless.core.caption;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public final class Caption {

    private static final HashMap<Language, JsonObject> languageJsonObjectHashMap = new HashMap<>();
    private static Language currentLanguage = Language.ENGLISH;

    private Caption() {
    }

    public static @NotNull Component of(String key, TagResolver... replacements) {
        String message = getString(key);

        message = message.replace("<prefix>", getString("prefix"));

        return MiniMessage.miniMessage().deserialize(message, replacements);
    }

    public static @NotNull String getAsLegacy(String key, TagResolver... replacements) {
        Component component = of(key, replacements);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static void loadLanguage(Language language, JsonObject jsonObject) {
        languageJsonObjectHashMap.put(language, jsonObject);
    }

    public static String getString(String key) {
        JsonObject languageObject = languageJsonObjectHashMap.get(currentLanguage);
        if (!languageObject.has(key)) {
            return "<prefix><red>Error - No such key: " + key;
        }
        return languageObject.get(key).getAsString();
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(Language newLanguage) {
        if (newLanguage != getCurrentLanguage()) {
            Caption.currentLanguage = newLanguage;
        }
    }
}
