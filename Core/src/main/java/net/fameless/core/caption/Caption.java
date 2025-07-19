package net.fameless.core.caption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fameless.core.util.PluginPaths;
import net.fameless.core.util.ResourceUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;

public final class Caption {

    private static final Logger LOGGER = LoggerFactory.getLogger("BungeeAFK/" + Caption.class.getSimpleName());
    private static final HashMap<Language, JsonObject> languageJsonObjectHashMap = new HashMap<>();
    private static Language currentLanguage = Language.ENGLISH;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

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
        LOGGER.info("Successfully loaded language: {}", language.getIdentifier());
    }

    public static void loadDefaultLanguages() {
        for (Language language : Language.values()) {
            File langFile = PluginPaths.getLangFile(language);

            if (!langFile.exists()) {
                ResourceUtil.extractResourceIfMissing("lang_" + language.getIdentifier() + ".json", langFile);
            }

            JsonObject jsonObject;
            try (FileReader reader = new FileReader(langFile)) {
                jsonObject = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load language file: " + langFile.getPath(), e);
            }

            loadLanguage(language, jsonObject);
        }
    }

    public static String getString(String key) {
        JsonObject languageObject = languageJsonObjectHashMap.get(currentLanguage);
        if (!languageObject.has(key)) {
            return "<prefix><red>Error - No such key: " + key;
        }
        return languageObject.get(key).getAsString();
    }

    public static String getString(Language language, String key) {
        JsonObject languageObject = languageJsonObjectHashMap.get(language);
        if (languageObject == null || !languageObject.has(key)) {
            return "<prefix><red>Error - No such key: " + key;
        }
        return languageObject.get(key).getAsString();
    }

    public static boolean hasKey(Language language, String key) {
        JsonObject languageObject = languageJsonObjectHashMap.get(language);
        return languageObject != null && languageObject.has(key);
    }

    public static JsonObject getLanguageJsonObject(Language language) {
        return languageJsonObjectHashMap.get(language);
    }

    public static Language getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(Language newLanguage) {
        if (newLanguage != getCurrentLanguage()) {
            Caption.currentLanguage = newLanguage;
        }
    }

    public static void saveToFile() {
        for (Language language : Language.values()) {
            File langFile = PluginPaths.getLangFile(language);
            JsonObject jsonObject = languageJsonObjectHashMap.get(language);

            if (jsonObject != null) {
                try (FileWriter writer = new FileWriter(langFile)) {
                    GSON.toJson(jsonObject, writer);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save language file: " + langFile.getPath(), e);
                }
            }
        }
    }
}
