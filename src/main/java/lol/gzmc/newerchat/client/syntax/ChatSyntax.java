package lol.gzmc.newerchat.client.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lol.gzmc.newerchat.client.ChatData;
import lol.gzmc.newerchat.config.NewerChatConfig;

public final class ChatSyntax {

    private ChatSyntax() {}

    private static final Pattern NUMBER = Pattern.compile("^~?-?\\d+(\\.\\d+)?$|^~$|^~-?\\d+(\\.\\d+)?$");
    private static final Pattern SELECTOR = Pattern.compile("^@[apre](\\[[^\\]]*\\])?$");

    public static final class Tok {
        public final int start;
        public final int end;
        public final int index;
        public final String text;

        Tok(int start, int end, int index, String text) {
            this.start = start;
            this.end = end;
            this.index = index;
            this.text = text;
        }
    }

    public static final class Seg {
        public final int start;
        public final int end;
        public final int color;

        Seg(int start, int end, int color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }
    }

    public static List<Tok> tokenize(String s) {
        List<Tok> out = new ArrayList<Tok>();
        int i = 0;
        int n = s.length();
        int idx = 0;
        while (i < n) {
            while (i < n && s.charAt(i) == ' ') {
                i++;
            }
            if (i >= n) {
                break;
            }
            int start = i;
            while (i < n && s.charAt(i) != ' ') {
                i++;
            }
            out.add(new Tok(start, i, idx++, s.substring(start, i)));
        }
        return out;
    }

    public static Tok tokenAt(String s, int cursor) {
        if (cursor < 0) {
            cursor = 0;
        }
        if (cursor > s.length()) {
            cursor = s.length();
        }
        List<Tok> toks = tokenize(s);
        for (Tok t : toks) {
            if (cursor >= t.start && cursor <= t.end) {
                return t;
            }
        }

        int idx = 0;
        for (Tok t : toks) {
            if (t.end <= cursor) {
                idx++;
            }
        }
        return new Tok(cursor, cursor, idx, "");
    }

    public static List<Seg> colorize(String s) {
        List<Seg> out = new ArrayList<Seg>();
        if (s.isEmpty()) {
            return out;
        }

        boolean isCommand = s.charAt(0) == '/';
        List<Tok> toks = tokenize(s);

        for (Tok t : toks) {
            int color;
            if (isCommand && t.index == 0) {
                String name = t.text.startsWith("/") ? t.text.substring(1) : t.text;
                color = ChatData.isKnownCommand(name)
                        ? NewerChatConfig.colCommandKnown
                        : NewerChatConfig.colCommandUnknown;
            } else {
                color = argColor(t.text, isCommand);
            }
            out.add(new Seg(t.start, t.end, color));
        }
        return out;
    }

    private static int argColor(String tok, boolean isCommand) {
        if (tok.isEmpty()) {
            return NewerChatConfig.colText;
        }
        if (isCommand && (NUMBER.matcher(tok).matches())) {
            return NewerChatConfig.colNumber;
        }
        if (isCommand && SELECTOR.matcher(tok).matches()) {
            return NewerChatConfig.colSelector;
        }
        if (ChatData.isOnlinePlayer(tok)) {
            return NewerChatConfig.colPlayer;
        }
        char c0 = tok.charAt(0);
        if (isCommand && (c0 == '{' || c0 == '[' || c0 == '"')) {
            return NewerChatConfig.colString;
        }
        return NewerChatConfig.colText;
    }
}
