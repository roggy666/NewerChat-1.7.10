package lol.gzmc.newerchat.client.suggestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lol.gzmc.newerchat.client.ChatData;
import lol.gzmc.newerchat.client.syntax.ChatSyntax;
import lol.gzmc.newerchat.config.NewerChatConfig;

public final class SuggestionProvider {

    private SuggestionProvider() {}

    public static ChatSyntax.Tok currentToken(String text, int cursor) {
        return ChatSyntax.tokenAt(text, cursor);
    }

    public static List<Suggestion> build(String text, int cursor) {
        ChatSyntax.Tok tok = ChatSyntax.tokenAt(text, cursor);
        String q = tok.text;
        List<Suggestion> out = new ArrayList<Suggestion>();

        if (q.isEmpty()) {
            return out;
        }

        boolean isCommand = text.charAt(0) == '/';

        if (isCommand && tok.index == 0) {
            String bare = q.startsWith("/") ? q.substring(1) : q;
            for (String name : ChatData.commandNames()) {
                if (matches(name, bare)) {
                    String usage = ChatData.clientCommandUsage(name);
                    out.add(new Suggestion("/" + name, usage));
                }
            }
        } else {

            if (isCommand) {
                for (String sel : ChatData.SELECTORS) {
                    if (matches(sel, q)) {
                        out.add(new Suggestion(sel, null));
                    }
                }
            }
            if (isCommand || NewerChatConfig.suggestPlayersInChat) {
                for (String p : ChatData.onlinePlayers()) {
                    if (matches(p, q)) {
                        out.add(new Suggestion(p, null));
                    }
                }
            }
        }

        sortByRelevance(out, q);
        return out;
    }

    public static List<Suggestion> merge(List<Suggestion> server, List<Suggestion> local, String query) {
        Map<String, Suggestion> map = new LinkedHashMap<String, Suggestion>();
        for (Suggestion s : server) {
            map.put(s.insert.toLowerCase(), s);
        }
        for (Suggestion s : local) {
            if (!map.containsKey(s.insert.toLowerCase())) {
                map.put(s.insert.toLowerCase(), s);
            }
        }
        List<Suggestion> out = new ArrayList<Suggestion>(map.values());
        sortByRelevance(out, query);
        return out;
    }

    public static boolean matches(String candidate, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        return candidate.regionMatches(true, 0, query, 0, query.length())
                || candidate.toLowerCase().contains(query.toLowerCase());
    }

    /** Drops suggestions that no longer match what the user has typed for this token. */
    public static List<Suggestion> filterByQuery(List<Suggestion> list, String query) {
        if (query == null || query.isEmpty()) {
            return list;
        }
        String q = query.startsWith("/") ? query.substring(1) : query;
        List<Suggestion> out = new ArrayList<Suggestion>();
        for (Suggestion s : list) {
            if (matches(s.insert, query) || matches(s.insert, q) || matches(s.display, q)) {
                out.add(s);
            }
        }
        return out;
    }

    private static void sortByRelevance(List<Suggestion> list, final String query) {
        final String q = query == null ? "" : query.toLowerCase();
        Collections.sort(list, new Comparator<Suggestion>() {
            @Override
            public int compare(Suggestion a, Suggestion b) {
                int ra = rank(a.display, q);
                int rb = rank(b.display, q);
                if (ra != rb) {
                    return ra - rb;
                }
                return a.display.compareToIgnoreCase(b.display);
            }
        });
    }

    private static int rank(String display, String q) {
        if (q.isEmpty()) {
            return 1;
        }
        String d = display.toLowerCase();
        String dNoSlash = d.startsWith("/") ? d.substring(1) : d;
        if (d.equals(q) || dNoSlash.equals(q)) {
            return 0;
        }
        if (d.startsWith(q) || dNoSlash.startsWith(q)) {
            return 1;
        }
        return 2;
    }

    public static int matchedPrefixLen(String display, String query) {
        if (query == null || query.isEmpty()) {
            return 0;
        }
        String q = query.startsWith("/") ? query.substring(1) : query;
        String d = display.startsWith("/") ? display.substring(1) : display;
        int offset = display.startsWith("/") ? 1 : 0;
        if (d.regionMatches(true, 0, q, 0, q.length())) {
            return offset + q.length();
        }
        return 0;
    }
}
