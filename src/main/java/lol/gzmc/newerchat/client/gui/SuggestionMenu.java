package lol.gzmc.newerchat.client.gui;

import java.util.ArrayList;
import java.util.List;

import lol.gzmc.newerchat.client.suggestion.Suggestion;
import lol.gzmc.newerchat.client.suggestion.SuggestionProvider;
import lol.gzmc.newerchat.config.NewerChatConfig;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class SuggestionMenu {

    private static final int ROW_H = 12;

    private final FontRenderer font;

    private List<Suggestion> items = new ArrayList<Suggestion>();
    private String query = "";
    private int selected = 0;
    private int scroll = 0;
    private boolean visible = false;

    private int lastX, lastW, lastTop, lastRows;

    public SuggestionMenu(FontRenderer font) {
        this.font = font;
    }

    public boolean isVisible() {
        return visible && !items.isEmpty();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void hide() {
        visible = false;
    }

    public List<Suggestion> all() {
        return items;
    }

    public Suggestion getSelected() {
        return items.isEmpty() ? null : items.get(clamp(selected, 0, items.size() - 1));
    }

    public void set(List<Suggestion> list, String query) {
        this.items = list != null ? list : new ArrayList<Suggestion>();
        this.query = query == null ? "" : query;
        this.selected = 0;
        this.scroll = 0;
        this.visible = !items.isEmpty();
    }

    public void move(int dir) {
        if (items.isEmpty()) {
            return;
        }
        int n = items.size();
        selected = ((selected + dir) % n + n) % n;
    }

    public void page(int dir) {
        if (items.isEmpty()) {
            return;
        }
        int rows = visibleRows();
        selected = clamp(selected + dir * rows, 0, items.size() - 1);
    }

    private int visibleRows() {
        return Math.min(items.size(), Math.max(1, NewerChatConfig.maxSuggestions));
    }

    public void render(int guiWidth, int guiHeight, int mouseX, int mouseY, int anchorX) {
        if (!isVisible()) {
            return;
        }
        int rows = visibleRows();

        if (selected < scroll) {
            scroll = selected;
        }
        if (selected >= scroll + rows) {
            scroll = selected - rows + 1;
        }
        scroll = clamp(scroll, 0, Math.max(0, items.size() - rows));

        int w = 20;
        for (int i = 0; i < rows; i++) {
            w = Math.max(w, font.getStringWidth(items.get(scroll + i).display) + 2);
        }

        int x = clamp(anchorX, 2, Math.max(2, guiWidth - 2 - w));
        int bottom = guiHeight - 14;
        int top = bottom - rows * ROW_H;

        lastX = x;
        lastW = w;
        lastTop = top;
        lastRows = rows;

        Gui.drawRect(x - 1, top - 1, x + w + 1, bottom, NewerChatConfig.boxBg);

        for (int i = 0; i < rows; i++) {
            int idx = scroll + i;
            int ry = top + i * ROW_H;
            Suggestion s = items.get(idx);

            boolean hovered = mouseX >= x - 1 && mouseX <= x + w + 1 && mouseY >= ry && mouseY < ry + ROW_H;
            if (idx == selected || hovered) {
                Gui.drawRect(x - 1, ry, x + w + 1, ry + ROW_H, NewerChatConfig.selectedColor);
            }

            int mlen = SuggestionProvider.matchedPrefixLen(s.display, query);
            int tx = x + 1;
            int tyText = ry + 2;
            if (mlen > 0 && mlen <= s.display.length()) {
                String head = s.display.substring(0, mlen);
                String tail = s.display.substring(mlen);
                font.drawStringWithShadow(head, tx, tyText, NewerChatConfig.colMatch);
                font.drawStringWithShadow(tail, tx + font.getStringWidth(head), tyText, NewerChatConfig.colText);
            } else {
                font.drawStringWithShadow(s.display, tx, tyText, NewerChatConfig.colText);
            }
        }

        if (items.size() > rows) {
            int trackH = rows * ROW_H;
            int barH = Math.max(4, trackH * rows / items.size());
            int maxScroll = items.size() - rows;
            int barY = top + (maxScroll == 0 ? 0 : (trackH - barH) * scroll / maxScroll);
            Gui.drawRect(x + w, top, x + w + 1, bottom, 0x40FFFFFF);
            Gui.drawRect(x + w, barY, x + w + 1, barY + barH, 0xFFBBBBBB);
        }

        if (NewerChatConfig.showTooltips) {
            Suggestion sel = getSelected();
            if (sel != null && sel.description != null && !sel.description.isEmpty()) {
                int tw = font.getStringWidth(sel.description) + 4;
                int tx = x + w + 4;
                if (tx + tw > guiWidth) {
                    tx = Math.max(2, x - tw - 4);
                }
                Gui.drawRect(tx - 1, top - 1, tx + tw, top + 11, NewerChatConfig.boxBg);
                font.drawStringWithShadow(sel.description, tx + 1, top + 1, 0xFFDDDDDD);
            }
        }
    }

    public int rowAt(int mouseX, int mouseY) {
        if (!isVisible()) {
            return -1;
        }
        if (mouseX < lastX - 1 || mouseX > lastX + lastW + 1) {
            return -1;
        }
        for (int i = 0; i < lastRows; i++) {
            int ry = lastTop + i * ROW_H;
            if (mouseY >= ry && mouseY < ry + ROW_H) {
                return scroll + i;
            }
        }
        return -1;
    }

    public Suggestion get(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex >= items.size()) {
            return null;
        }
        return items.get(absoluteIndex);
    }

    public void setSelectedAbsolute(int idx) {
        selected = clamp(idx, 0, Math.max(0, items.size() - 1));
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
