package com.example.newerchat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.command.ICommand;
import net.minecraftforge.client.ClientCommandHandler;

public final class ChatData {

    private ChatData() {}

    public static final Set<String> VANILLA = new HashSet<String>(Arrays.asList(
            "help", "seed", "list", "me", "tell", "msg", "w", "say", "kill",
            "gamemode", "gamerule", "give", "tp", "kick", "ban", "ban-ip", "banlist",
            "pardon", "pardon-ip", "op", "deop", "difficulty", "spawnpoint",
            "setworldspawn", "weather", "time", "xp", "defaultgamemode", "enchant",
            "clear", "testfor", "spreadplayers", "playsound", "scoreboard", "effect",
            "particle", "summon", "setblock", "clone", "achievement", "setidletimeout",
            "save-all", "save-off", "save-on", "stop", "whitelist", "publish", "debug"
    ));

    private static final Set<String> LEARNED = Collections.synchronizedSet(new LinkedHashSet<String>());

    public static void learnServerCommands(String[] names) {
        if (names == null) {
            return;
        }
        for (String n : names) {
            if (n == null || n.isEmpty()) {
                continue;
            }
            LEARNED.add(n.startsWith("/") ? n.substring(1) : n);
        }
    }

    public static Set<String> commandNames() {
        Set<String> out = new TreeSet<String>(VANILLA);
        try {
            Map<?, ?> cmds = ClientCommandHandler.instance.getCommands();
            for (Object key : cmds.keySet()) {
                out.add(String.valueOf(key));
            }
        } catch (Throwable ignored) {

        }
        synchronized (LEARNED) {
            out.addAll(LEARNED);
        }
        return out;
    }

    public static boolean isKnownCommand(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String lower = name.toLowerCase();
        for (String c : commandNames()) {
            if (c.toLowerCase().equals(lower)) {
                return true;
            }
        }
        return false;
    }

    public static String clientCommandUsage(String name) {
        try {
            ICommand c = (ICommand) ClientCommandHandler.instance.getCommands().get(name);
            if (c != null) {
                return c.getCommandUsage(Minecraft.getMinecraft().thePlayer);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> onlinePlayers() {
        List<String> out = new ArrayList<String>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) {
            return out;
        }
        try {
            List<GuiPlayerInfo> infos = mc.thePlayer.sendQueue.playerInfoList;
            for (GuiPlayerInfo info : infos) {
                if (info != null && info.name != null && !info.name.isEmpty()) {
                    out.add(info.name);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    public static boolean isOnlinePlayer(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (String p : onlinePlayers()) {
            if (p.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static final List<String> SELECTORS =
            Collections.unmodifiableList(Arrays.asList("@p", "@a", "@r", "@e"));
}
