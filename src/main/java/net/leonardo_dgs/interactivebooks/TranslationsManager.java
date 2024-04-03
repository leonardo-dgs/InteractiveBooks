package net.leonardo_dgs.interactivebooks;

import de.leonhard.storage.SimplixBuilder;
import de.leonhard.storage.Yaml;
import de.leonhard.storage.internal.settings.DataType;
import de.leonhard.storage.internal.settings.ReloadSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

final class TranslationsManager {
    private static final String langPath = "translations/";
    private final File langFolder;
    private final SettingsManager settings;
    private final HashMap<String, Yaml> langConfigs = new HashMap<>();

    TranslationsManager(File langFolder, SettingsManager settings) {
        this.langFolder = langFolder;
        this.settings = settings;
        if (!langFolder.exists() && !langFolder.mkdirs())
            throw new RuntimeException();

        String defaultLang = settings.getDefaultLanguage();
        langConfigs.put(defaultLang, getLangConfig(defaultLang));
    }

    Component getMessage(String key, String langCode, TagResolver.Single... substitutions) {
        String rawMessage = getString(key, langCode);
        if (rawMessage.isEmpty())
            return null;
        if (langCode == null || !settings.getPerPlayerLanguage())
            langCode = settings.getDefaultLanguage();

        List<TagResolver.Single> subsWithPrefix = new ArrayList<>(substitutions.length + 1);
        if (!key.equals("prefix")) {
            TagResolver.Single prefixPlaceholder = Placeholder.parsed("prefix", getString("prefix", langCode));
            subsWithPrefix.add(prefixPlaceholder);
        }
        subsWithPrefix.addAll(Arrays.asList(substitutions));
        return MiniMessage.miniMessage().deserialize(rawMessage, subsWithPrefix.toArray(new TagResolver.Single[0]));
    }

    Component getHelpHeader(String langCode, TagResolver.Single... substitutions) {
        return getMessage("help.header", langCode, substitutions);
    }

    Component getHelpList(String langCode, TagResolver.Single... substitutions) {
        return getMessage("help.list", langCode, substitutions);
    }

    Component getHelpOpen(String langCode, TagResolver.Single... substitutions) {
        return getMessage("help.open", langCode, substitutions);
    }

    Component getHelpGet(String langCode, TagResolver.Single... substitutions) {
        return getMessage("help.get", langCode, substitutions);
    }

    Component getHelpGive(String langCode, TagResolver.Single... substitutions) {
        return getMessage("help.give", langCode, substitutions);
    }

    Component getHelpCreate(String langCode, TagResolver.Single... substitutions) {
        return getMessage("help.create", langCode, substitutions);
    }

    Component getHelpReload(String langCode, TagResolver.Single... substitutions) {
        return getMessage("help.reload", langCode, substitutions);
    }

    Component getBookListHeader(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_list.header", langCode, substitutions);
    }

    Component getBookList(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_list.book", langCode, substitutions);
    }

    Component getBookListSeparator(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_list.separator", langCode, substitutions);
    }

    Component getBookOpenUsage(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_open.usage", langCode, substitutions);
    }

    Component getBookOpenPlayerNotSpecified(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_open.player_not_specified", langCode, substitutions);
    }

    Component getBookOpenSuccess(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_open.success", langCode, substitutions);
    }

    Component getBookDoesNotExists(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_does_not_exists", langCode, substitutions);
    }

    Component getPlayerNotConnected(String langCode, TagResolver.Single... substitutions) {
        return getMessage("player_not_connected", langCode, substitutions);
    }

    Component getBookGetUsage(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_get.usage", langCode, substitutions);
    }

    Component getBookGetSuccess(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_get.success", langCode, substitutions);
    }

    Component getBookGiveUsage(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_give.usage", langCode, substitutions);
    }

    Component getBookGiveSuccess(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_give.success", langCode, substitutions);
    }

    Component getBookReceived(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_give.received", langCode, substitutions);
    }

    Component getBookCreateUsage(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_create.usage", langCode, substitutions);
    }

    Component getBookAlreadyExists(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_create.already_exists", langCode, substitutions);
    }

    Component getBookCreateInvalidGeneration(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_create.invalid_generation", langCode, substitutions);
    }

    Component getBookCreateSuccess(String langCode, TagResolver.Single... substitutions) {
        return getMessage("book_create.success", langCode, substitutions);
    }

    Component getReloadSuccess(String langCode, TagResolver.Single... substitutions) {
        return getMessage("reload_success", langCode, substitutions);
    }

    private String getString(String key, String langCode) {
        Yaml langConfig = getLangConfig(langCode);
        if (!langConfig.contains(key)) {
            langConfig = getLangConfig(settings.getDefaultLanguage());
            if (!langConfig.contains(key)) {
                langConfig = getLangConfig("en_us");
            }
        }
        return langConfig.getString(key);
    }

    private Yaml getLangConfig(String langCode) {
        Yaml langConfig = loadLangConfig(langCode);
        if (langConfig == null) {
            langConfig = loadLangConfig(settings.getDefaultLanguage());
            if (langConfig == null) {
                langConfig = loadLangConfig("en_us");
            }
        }

        return langConfig;
    }

    private Yaml loadLangConfig(String langCode) {
        Yaml langConfig = langConfigs.get(langCode);
        if (langConfig != null)
            return langConfig;

        File file = new File(langFolder.toPath().toString(), langCode + ".yml");
        InputStream resource = getClass().getClassLoader().getResourceAsStream(langPath + langCode + ".yml");
        if (resource == null && !file.exists())
            return null;

        langConfig = SimplixBuilder.fromFile(file)
                .setReloadSettings(ReloadSettings.INTELLIGENT)
                .setDataType(DataType.SORTED)
                .createYaml();
        langConfig.addDefaultsFromInputStream(resource);

        langConfigs.put(langCode, langConfig);
        return langConfig;
    }
}
