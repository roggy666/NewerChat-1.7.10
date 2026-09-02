package lol.gzmc.newerchat.config;

import java.io.File;

import lol.gzmc.newerchat.NewerChat;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

public class NewerChatConfig {

    public static boolean enabled = true;
    public static boolean syntaxHighlighting = true;
    public static boolean queryServer = true;
    public static boolean showTooltips = true;
    public static boolean suggestPlayersInChat = true;
    public static int maxSuggestions = 10;
    public static int debounceMs = 140;

    public static int boxBg = 0xE6101014;
    public static int selectedColor = 0xFF2A4A78;
    public static int barBg = 0x80000000;

    public static int colText = 0xFFE0E0E0;
    public static int colMatch = 0xFFFFFF55;
    public static int colCommandKnown = 0xFF55FF55;
    public static int colCommandUnknown = 0xFFFF5555;
    public static int colNumber = 0xFF55FFFF;
    public static int colSelector = 0xFFFFAA00;
    public static int colPlayer = 0xFF55FFFF;
    public static int colString = 0xFFFFAA55;

    private static Configuration config;

    public static void load(File file) {
        config = new Configuration(file);
        sync();
    }

    public static void sync() {
        if (config == null) {
            return;
        }

        final String CAT_GENERAL = "general";
        final String CAT_APPEARANCE = "appearance";
        final String CAT_SYNTAX = "syntax";

        enabled = config.getBoolean("enabled", CAT_GENERAL, enabled,
                "Replace the vanilla chat screen with NewerChat");
        syntaxHighlighting = config.getBoolean("syntaxHighlighting", CAT_GENERAL, syntaxHighlighting,
                "Colour the command as you type it");
        queryServer = config.getBoolean("queryServer", CAT_GENERAL, queryServer,
                "Ask the server for completions (tab-complete packet) while typing");
        showTooltips = config.getBoolean("showTooltips", CAT_GENERAL, showTooltips,
                "Show the usage string next to the selected suggestion");
        suggestPlayersInChat = config.getBoolean("suggestPlayersInChat", CAT_GENERAL, suggestPlayersInChat,
                "Suggest player names in plain messages, not only in commands");
        maxSuggestions = config.getInt("maxSuggestions", CAT_GENERAL, maxSuggestions, 1, 15,
                "Rows shown in the suggestion list at once");
        debounceMs = config.getInt("debounceMs", CAT_GENERAL, debounceMs, 0, 1000,
                "Delay before the server is queried after the last keypress, ms");

        boxBg = hex(config.getString("boxBackground", CAT_APPEARANCE, argb(boxBg),
                "Suggestion box background, ARGB hex"));
        selectedColor = hex(config.getString("selectedRow", CAT_APPEARANCE, argb(selectedColor),
                "Selected / hovered row highlight, ARGB hex"));
        barBg = hex(config.getString("inputBarBackground", CAT_APPEARANCE, argb(barBg),
                "Input bar background, ARGB hex"));
        colText = hex(config.getString("text", CAT_APPEARANCE, argb(colText),
                "Main text colour, ARGB hex"));
        colMatch = hex(config.getString("matchedPrefix", CAT_APPEARANCE, argb(colMatch),
                "Colour of the matched prefix in a suggestion, ARGB hex"));

        colCommandKnown = hex(config.getString("commandKnown", CAT_SYNTAX, argb(colCommandKnown),
                "Known command colour, ARGB hex"));
        colCommandUnknown = hex(config.getString("commandUnknown", CAT_SYNTAX, argb(colCommandUnknown),
                "Unknown command colour, ARGB hex"));
        colNumber = hex(config.getString("number", CAT_SYNTAX, argb(colNumber),
                "Numbers and coordinates colour, ARGB hex"));
        colSelector = hex(config.getString("selector", CAT_SYNTAX, argb(colSelector),
                "Target selector colour (@a, @p ...), ARGB hex"));
        colPlayer = hex(config.getString("playerName", CAT_SYNTAX, argb(colPlayer),
                "Online player name colour, ARGB hex"));
        colString = hex(config.getString("string", CAT_SYNTAX, argb(colString),
                "Quoted / JSON argument colour, ARGB hex"));

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static int hex(String s) {
        try {
            String clean = s.trim().replace("#", "").replace("0x", "").replace("0X", "");
            return (int) Long.parseLong(clean, 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    private static String argb(int color) {
        return String.format("%08X", color);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (NewerChat.MODID.equals(event.modID)) {
            sync();
        }
    }
}
