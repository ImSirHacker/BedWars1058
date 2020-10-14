package com.andrei1058.bedwars.api.language;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigManager;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.player.PlayerLangChangeEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public class Language extends ConfigManager {

    private String iso, prefix = "";
    private static HashMap<Player, Language> langByPlayer = new HashMap<>();
    private static List<Language> languages = new ArrayList<>();
    private static Language defaultLanguage;

    public Language(Plugin plugin, String iso) {
        super(plugin, "messages_" + iso, plugin.getDataFolder().getPath() + "/Languages");
        this.iso = iso;
        languages.add(this);
    }

    /**
     * Set chat prefix.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Get scoreboard strings.
     */
    public static List<String> getScoreboard(Player p, String path, String alternative) {
        Language language = getPlayerLanguage(p);
        if (language.exists(path)) {
            return language.l(path);
        } else {
            if (path.split("\\.").length == 3) {
                String[] sp = path.split("\\.");
                String path2 = sp[1];
                path2 = String.valueOf(path2.charAt(0)).toUpperCase() + path2.substring(1).toLowerCase();
                path2 = sp[0] + "." + path2 + "." + sp[2];
                if (language.exists(path2)) {
                    return language.l(path);
                } else if (language.exists(sp[0] + "." + sp[1].toUpperCase() + "." + sp[2])) {
                    return language.l(sp[0] + "." + sp[1].toUpperCase() + "." + sp[2]);
                }
            }
        }
        return language.l(alternative);
    }

    /**
     * Get language display name.
     */
    public String getLangName() {
        return getYml().getString("name");
    }

    /**
     * Get message in player's language.
     */
    public static String getMsg(Player p, String path) {
        return langByPlayer.getOrDefault(p, getDefaultLanguage()).m(path);
    }

    /**
     * Retrieve a player language.
     */
    public static Language getPlayerLanguage(Player p) {
        return langByPlayer.getOrDefault(p, getDefaultLanguage());
    }

    /**
     * Check if a message was set.
     */
    public boolean exists(String path) {
        return getYml().get(path) != null;
    }

    /**
     * Get a string list in player's language.
     */
    public static List<String> getList(Player p, String path) {
        return langByPlayer.getOrDefault(p, getDefaultLanguage()).l(path);
    }

    /**
     * Save a value to file if not exists.
     */
    public static void saveIfNotExists(String path, Object data) {
        for (Language l : languages) {
            if (l.getYml().get(path) == null) {
                l.set(path, data);
            }
        }
    }

    /**
     * Get a color translated message.
     */
    public String m(String path) {
        return ChatColor.translateAlternateColorCodes('&', getYml().getString(path).replace("{prefix}", prefix));
    }

    /**
     * Get a color translated list.
     */
    public List<String> l(String path) {
        List<String> result = new ArrayList<>();
        for (String line : getYml().getStringList(path)) {
            result.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return result;
    }

    public static HashMap<Player, Language> getLangByPlayer() {
        return langByPlayer;
    }

    /**
     * Check if a language exists.
     */
    public static boolean isLanguageExist(String iso) {
        for (Language l : languages) {
            if (l.iso.equalsIgnoreCase(iso)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get language with given info.
     *
     * @return null if could not find.
     */
    public static Language getLang(String iso) {
        for (Language l : languages) {
            if (l.iso.equalsIgnoreCase(iso)) {
                return l;
            }
        }
        return getDefaultLanguage();
    }

    /**
     * Get language iso code.
     */
    public String getIso() {
        return iso;
    }

    /**
     * Get loaded languages list.
     */
    public static List<Language> getLanguages() {
        return languages;
    }

    /**
     * Save messages for unset stats items.
     */
    public static void setupCustomStatsMessages() {
        BedWars api = Bukkit.getServer().getServicesManager().getRegistration(BedWars.class).getProvider();
        for (Language l : getLanguages()) {
            if (l == null) continue;
            if (l.getYml() == null) continue;
            /* save messages for stats gui items if custom items added */
            if (api.getConfigs().getMainConfig().getYml().get("ConfigPath.GENERAL_CONFIGURATION_STATS_PATH") == null)
                return;
            for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_STATS_PATH).getKeys(false)) {
                if (ConfigPath.GENERAL_CONFIGURATION_STATS_GUI_SIZE.contains(item)) continue;
                if (l.getYml().getDefaults() == null || !l.getYml().getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-name"))
                    l.getYml().addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-name", "Name not set");
                if (l.getYml().getDefaults() == null || !l.getYml().getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-lore"))
                    l.getYml().addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + item + "-lore", Collections.singletonList("lore not set"));
            }
            l.save();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void addDefaultStatsMsg(YamlConfiguration yml, String path, String name, String... lore) {
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-name"))
            yml.addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-name", name);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-lore"))
            yml.addDefault(Messages.PLAYER_STATS_GUI_PATH + "-" + path + "-lore", lore);
    }

    /**
     * Create missing name/ lore for items: multi arena lobby, waiting, spectating
     */
    public static void addDefaultMessagesCommandItems(Language language) {
        if (language == null) return;
        YamlConfiguration yml = language.getYml();
        if (yml == null) return;
        BedWars api = Bukkit.getServer().getServicesManager().getRegistration(BedWars.class).getProvider();
        if (api.getConfigs().getMainConfig().getYml().get(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH) != null) {
            for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_LOBBY_ITEMS_PATH).getKeys(false)) {
                if (item.isEmpty()) continue;
                String p1 = Messages.GENERAL_CONFIGURATION_LOBBY_ITEMS_NAME.replace("%path%", item);
                String p2 = Messages.GENERAL_CONFIGURATION_LOBBY_ITEMS_LORE.replace("%path%", item);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p1, "&cName not set at: &f" + p1);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p2, Arrays.asList("&cLore not set at:", " &f" + p2));
            }
        }
        if (api.getConfigs().getMainConfig().getYml().get(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH) != null) {
            for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_PATH).getKeys(false)) {
                if (item.isEmpty()) continue;
                String p1 = Messages.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_NAME.replace("%path%", item);
                String p2 = Messages.GENERAL_CONFIGURATION_SPECTATOR_ITEMS_LORE.replace("%path%", item);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p1, "&cName not set at: &f" + p1);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p2, Arrays.asList("&cLore not set at:", " &f" + p2));
            }
        }
        if (api.getConfigs().getMainConfig().getYml().get(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH) != null) {
            for (String item : api.getConfigs().getMainConfig().getYml().getConfigurationSection(ConfigPath.GENERAL_CONFIGURATION_PRE_GAME_ITEMS_PATH).getKeys(false)) {
                if (item.isEmpty()) continue;
                String p1 = Messages.GENERAL_CONFIGURATION_WAITING_ITEMS_NAME.replace("%path%", item);
                String p2 = Messages.GENERAL_CONFIGURATION_WAITING_ITEMS_LORE.replace("%path%", item);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p1, "&cName not set at: &f" + p1);
                if (yml.getDefaults() == null || !yml.getDefaults().contains(p1))
                    yml.addDefault(p2, Arrays.asList("&cLore not set at:", " &f" + p2));
            }
        }
        yml.options().copyDefaults(true);
        language.save();
    }

    /**
     * Create messages paths for new shop categories
     */
    @SuppressWarnings("DuplicateExpressions")
    public void setupUnSetCategories() {
        BedWars api = Bukkit.getServer().getServicesManager().getRegistration(BedWars.class).getProvider();
        for (String s : api.getConfigs().getShopConfig().getYml().getConfigurationSection("").getKeys(false)) {
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SETTINGS_PATH)) continue;
            if (s.equalsIgnoreCase(ConfigPath.SHOP_SPECIALS_PATH)) continue;
            if (s.equals(ConfigPath.SHOP_QUICK_DEFAULTS_PATH)) continue;
            if (!exists(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", s))) {
                set(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", s), "&8Name not set");
            }
            if (!exists(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", s))) {
                set(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", s), "&8Name not set");
            }
            if (!exists(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", s))) {
                set(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", s), Collections.singletonList("&8Lore not set"));
            }
            if (api.getConfigs().getShopConfig().getYml().get(s + ConfigPath.SHOP_CATEGORY_CONTENT_PATH) == null)
                continue;
            for (String c : api.getConfigs().getShopConfig().getYml().getConfigurationSection(s + ConfigPath.SHOP_CATEGORY_CONTENT_PATH).getKeys(false)) {
                if (!exists(Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", s).replace("%content%", c))) {
                    set(Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", s).replace("%content%", c), "&8Name not set");
                }
                if (!exists(Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", s).replace("%content%", c))) {
                    set(Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", s).replace("%content%", c), Collections.singletonList("&8Lore not set"));
                }
            }
        }
    }

    /**
     * Add required messages for a shop category to the given yml
     */
    public static void addCategoryMessages(YamlConfiguration yml, String categoryName, String invName, String itemName, List<String> itemLore) {
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", categoryName)))
            yml.addDefault(Messages.SHOP_CATEGORY_INVENTORY_NAME.replace("%category%", categoryName), invName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", categoryName)))
            yml.addDefault(Messages.SHOP_CATEGORY_ITEM_NAME.replace("%category%", categoryName), itemName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", categoryName)))
            yml.addDefault(Messages.SHOP_CATEGORY_ITEM_LORE.replace("%category%", categoryName), itemLore);
    }

    /**
     * Add required messages for a shop category to the given yml
     */
    public static void addContentMessages(YamlConfiguration yml, String contentName, String categoryName, String itemName, List<String> itemLore) {
        final String path1 = Messages.SHOP_CONTENT_TIER_ITEM_NAME.replace("%category%", categoryName).replace("%content%", contentName),
                path2 = Messages.SHOP_CONTENT_TIER_ITEM_LORE.replace("%category%", categoryName).replace("%content%", contentName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(path1)) yml.addDefault(path1, itemName);
        if (yml.getDefaults() == null || !yml.getDefaults().contains(path2)) yml.addDefault(path2, itemLore);
    }

    /**
     * Change a player language and refresh
     * scoreboard and custom join items.
     */
    public static boolean setPlayerLanguage(Player p, String iso, boolean onLogin) {

        Language newLang = Language.getLang(iso);
        if (newLang == null) return false;
        Language oldLang = Language.getPlayerLanguage(p);
        if (oldLang.getIso().equals(newLang.getIso())) return false;

        if (!onLogin) {
            PlayerLangChangeEvent e = new PlayerLangChangeEvent(p, oldLang.getIso(), newLang.getIso());
            Bukkit.getPluginManager().callEvent(e);
            if (e.isCancelled()) return false;
        }

        if (Language.getLangByPlayer().containsKey(p)) {
            Language.getLangByPlayer().replace(p, newLang);
        } else {
            Language.getLangByPlayer().put(p, newLang);
        }
        return true;
    }

    /**
     * Change server default language.
     */
    public static void setDefaultLanguage(Language defaultLanguage) {
        Language.defaultLanguage = defaultLanguage;
    }

    /**
     * Get server default language.
     */
    public static Language getDefaultLanguage() {
        return defaultLanguage;
    }
}
