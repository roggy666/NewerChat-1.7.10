package com.example.newerchat.client.gui;

import java.util.List;

import com.example.newerchat.client.syntax.ChatSyntax;
import com.example.newerchat.config.NewerChatConfig;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

public class ChatInputField {

    private final FontRenderer font;

    private String text = "";
    private int maxLength = 100;
    private int cursor = 0;
    private int selAnchor = 0;
    private int scroll = 0;
    private boolean focused = true;

    public boolean dirty = false;

    public ChatInputField(FontRenderer font) {
        this.font = font;
    }

    private int x() {
        return 4;
    }

    private int innerWidth(int guiWidth) {
        return guiWidth - 8;
    }

    private int baselineY(int guiHeight) {
        return guiHeight - 12 + 2;
    }

    public String getText() {
        return text;
    }

    public int getCursor() {
        return cursor;
    }

    public boolean hasSelection() {
        return cursor != selAnchor;
    }

    public int selStart() {
        return Math.min(cursor, selAnchor);
    }

    public int selEnd() {
        return Math.max(cursor, selAnchor);
    }

    public String getSelectedText() {
        return hasSelection() ? text.substring(selStart(), selEnd()) : "";
    }

    public void setMaxLength(int max) {
        this.maxLength = max;
        if (text.length() > max) {
            text = text.substring(0, max);
            clampCursor();
        }
    }

    public void setFocused(boolean f) {
        this.focused = f;
    }

    public void setText(String s) {
        this.text = filter(s);
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }
        cursor = selAnchor = text.length();
        scroll = 0;
        dirty = true;
    }

    public void setTextAndCursor(String s, int newCursor) {
        this.text = filter(s);
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }
        cursor = selAnchor = clamp(newCursor, 0, text.length());
        dirty = true;
    }

    public void setCursorToEnd() {
        cursor = selAnchor = text.length();
    }

    public void selectAll() {
        selAnchor = 0;
        cursor = text.length();
    }

    public boolean keyTyped(char typedChar, int key) {
        boolean ctrl = isCtrl();
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        switch (key) {
            case Keyboard.KEY_BACK:
                if (hasSelection()) {
                    deleteSelection();
                } else if (ctrl) {
                    deleteRange(prevWord(), cursor);
                } else if (cursor > 0) {
                    deleteRange(cursor - 1, cursor);
                }
                return true;

            case Keyboard.KEY_DELETE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (ctrl) {
                    deleteRange(cursor, nextWord());
                } else if (cursor < text.length()) {
                    deleteRange(cursor, cursor + 1);
                }
                return true;

            case Keyboard.KEY_LEFT:
                moveCursor(ctrl ? prevWord() : cursor - 1, shift);
                return true;

            case Keyboard.KEY_RIGHT:
                moveCursor(ctrl ? nextWord() : cursor + 1, shift);
                return true;

            case Keyboard.KEY_HOME:
                moveCursor(0, shift);
                return true;

            case Keyboard.KEY_END:
                moveCursor(text.length(), shift);
                return true;

            default:
                if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
                    insert(String.valueOf(typedChar));
                    return true;
                }
                return false;
        }
    }

    public void insert(String s) {
        String clean = filter(s);
        if (clean.isEmpty() && !s.isEmpty()) {
            return;
        }
        int start = selStart();
        int end = selEnd();
        int room = maxLength - (text.length() - (end - start));
        if (room <= 0) {
            return;
        }
        if (clean.length() > room) {
            clean = clean.substring(0, room);
        }
        text = text.substring(0, start) + clean + text.substring(end);
        cursor = selAnchor = start + clean.length();
        dirty = true;
    }

    public void deleteSelection() {
        if (hasSelection()) {
            deleteRange(selStart(), selEnd());
        }
    }

    private void deleteRange(int from, int to) {
        from = clamp(from, 0, text.length());
        to = clamp(to, 0, text.length());
        if (from >= to) {
            return;
        }
        text = text.substring(0, from) + text.substring(to);
        cursor = selAnchor = from;
        dirty = true;
    }

    private void moveCursor(int to, boolean keepSelection) {
        cursor = clamp(to, 0, text.length());
        if (!keepSelection) {
            selAnchor = cursor;
        }
    }

    private int prevWord() {
        int i = cursor;
        while (i > 0 && text.charAt(i - 1) == ' ') {
            i--;
        }
        while (i > 0 && text.charAt(i - 1) != ' ') {
            i--;
        }
        return i;
    }

    private int nextWord() {
        int i = cursor;
        int n = text.length();
        while (i < n && text.charAt(i) != ' ') {
            i++;
        }
        while (i < n && text.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    public void setCursorByX(int mouseX, int guiWidth) {
        int rel = mouseX - x();
        if (rel <= 0) {
            moveCursor(scroll, false);
            return;
        }
        String fromScroll = text.substring(Math.min(scroll, text.length()));
        String fit = font.trimStringToWidth(fromScroll, rel);
        moveCursor(scroll + fit.length(), false);
    }

    public void render(int guiWidth, int guiHeight, int cursorCounter) {
        int innerW = innerWidth(guiWidth);
        recomputeScroll(innerW);

        int left = x();
        int ty = baselineY(guiHeight);
        String fromScroll = text.substring(Math.min(scroll, text.length()));
        String visible = font.trimStringToWidth(fromScroll, innerW);
        int visEnd = scroll + visible.length();

        if (hasSelection()) {
            int a = clamp(selStart(), scroll, visEnd);
            int b = clamp(selEnd(), scroll, visEnd);
            if (b > a) {
                int sx = left + font.getStringWidth(text.substring(scroll, a));
                int ex = left + font.getStringWidth(text.substring(scroll, b));
                Gui.drawRect(sx, ty - 2, ex, ty + 10, 0x803399FF);
            }
        }

        if (NewerChatConfig.syntaxHighlighting) {
            List<ChatSyntax.Seg> segs = ChatSyntax.colorize(text);
            for (ChatSyntax.Seg seg : segs) {
                int a = Math.max(seg.start, scroll);
                int b = Math.min(seg.end, visEnd);
                if (b <= a) {
                    continue;
                }
                int px = left + font.getStringWidth(text.substring(scroll, a));
                font.drawStringWithShadow(text.substring(a, b), px, ty, seg.color);
            }
        } else {
            font.drawStringWithShadow(visible, left, ty, NewerChatConfig.colText);
        }

        if (focused && (cursorCounter / 6) % 2 == 0 && cursor >= scroll && cursor <= visEnd) {
            int cx = left + font.getStringWidth(text.substring(scroll, cursor));
            if (cursor < text.length()) {
                Gui.drawRect(cx, ty - 2, cx + 1, ty + 10, 0xFFD0D0D0);
            } else {
                font.drawStringWithShadow("_", cx, ty, 0xFFD0D0D0);
            }
        }
    }

    public int screenXOf(int index, int guiWidth) {
        recomputeScroll(innerWidth(guiWidth));
        int i = clamp(index, scroll, text.length());
        return x() + font.getStringWidth(text.substring(scroll, i));
    }

    private void recomputeScroll(int innerW) {
        clampCursor();
        if (scroll > text.length()) {
            scroll = text.length();
        }
        if (cursor < scroll) {
            scroll = cursor;
        }
        while (scroll < cursor && font.getStringWidth(text.substring(scroll, cursor)) > innerW) {
            scroll++;
        }
        while (scroll > 0 && font.getStringWidth(text.substring(scroll - 1)) <= innerW) {
            scroll--;
        }
    }

    private void clampCursor() {
        cursor = clamp(cursor, 0, text.length());
        selAnchor = clamp(selAnchor, 0, text.length());
    }

    private static boolean isCtrl() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private static String filter(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
