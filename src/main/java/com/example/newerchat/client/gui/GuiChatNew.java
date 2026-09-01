package com.example.newerchat.client.gui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.newerchat.client.ChatData;
import com.example.newerchat.client.suggestion.Suggestion;
import com.example.newerchat.client.suggestion.SuggestionProvider;
import com.example.newerchat.client.syntax.ChatSyntax;
import com.example.newerchat.config.NewerChatConfig;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class GuiChatNew extends GuiChat {

    private final String pendingText;

    private ChatInputField field;
    private SuggestionMenu menu;

    private int cursorCounter;

    private boolean pendingServerQuery;
    private long lastEditMs;

    private List<Suggestion> lastLocal = new ArrayList<Suggestion>();
    private List<Suggestion> lastServer = new ArrayList<Suggestion>();
    private String lastQuery = "";
    private int lastServerTokenIndex = -1;

    private ChatSyntax.Tok awaitingToken;

    private int historyIndex;
    private String historyStash = "";

    private boolean cycling = false;
    private String cycleBefore = "";
    private String cycleAfter = "";
    private List<Suggestion> cycleList = new ArrayList<Suggestion>();
    private int cycleIndex = 0;

    public GuiChatNew(String defaultText) {
        super(defaultText == null ? "" : defaultText);
        this.pendingText = defaultText == null ? "" : defaultText;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.field = new ChatInputField(this.fontRendererObj);
        this.field.setMaxLength(100);
        this.field.setText(this.pendingText);
        this.field.setCursorToEnd();

        this.menu = new SuggestionMenu(this.fontRendererObj);
        this.lastLocal = new ArrayList<Suggestion>();
        this.historyIndex = getSentMessages().size();

        onEdit();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);

        this.mc.ingameGUI.getChatGUI().resetScroll();
    }

    @Override
    public void updateScreen() {
        cursorCounter++;
        if (pendingServerQuery && System.currentTimeMillis() - lastEditMs >= NewerChatConfig.debounceMs) {
            pendingServerQuery = false;
            sendServerQuery();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            cycling = false;
            if (menu.isVisible()) {
                menu.hide();
            } else {
                this.mc.displayGuiScreen(null);
            }
            return;
        }

        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            sendMessage(field.getText());
            this.mc.displayGuiScreen(null);
            return;
        }

        if (key == Keyboard.KEY_TAB) {
            cycleCompletion(isShiftKeyDown() ? -1 : 1);
            return;
        }

        if (menu.isVisible() && (key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_NEXT)) {
            menu.page(key == Keyboard.KEY_PRIOR ? -1 : 1);
            return;
        }
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN) {
            navigateHistory(key == Keyboard.KEY_UP ? -1 : 1);
            return;
        }

        if (isCtrlDown()) {
            switch (key) {
                case Keyboard.KEY_A:
                    field.selectAll();
                    return;
                case Keyboard.KEY_C:
                    setClipboardString(field.getSelectedText());
                    return;
                case Keyboard.KEY_X:
                    setClipboardString(field.getSelectedText());
                    field.deleteSelection();
                    onEdit();
                    return;
                case Keyboard.KEY_V:
                    field.insert(getClipboardString());
                    onEdit();
                    return;
                default:
                    break;
            }
        }

        if (field.keyTyped(typedChar, key)) {
            onEdit();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        cycling = false;
        if (mouseButton == 0 && menu.isVisible()) {
            int row = menu.rowAt(mouseX, mouseY);
            if (row >= 0) {
                menu.setSelectedAbsolute(row);
                acceptSuggestion(menu.get(row));
                return;
            }
        }

        if (mouseButton == 0) {
            IChatComponent comp = this.mc.ingameGUI.getChatGUI()
                    .func_146236_a(Mouse.getX(), this.mc.displayHeight - Mouse.getY() - 1);
            if (comp != null && handleChatClick(comp)) {
                return;
            }
        }

        if (mouseY >= this.height - 14) {
            field.setCursorByX(mouseX, this.width);
        }
    }

    @Override
    public void handleMouseInput() {
        int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            int dir = dWheel > 0 ? 1 : -1;
            if (menu.isVisible() && menu.rowAt(mx, my) != -1) {
                menu.move(-dir);
            } else {
                this.mc.ingameGUI.getChatGUI().scroll(dir * (isShiftKeyDown() ? 1 : 7));
            }
        }

        int button = Mouse.getEventButton();
        if (button != -1 && Mouse.getEventButtonState()) {
            this.mouseClicked(mx, my, button);
        }
    }

    private void onEdit() {
        field.dirty = false;
        cycling = false;
        refreshLocal();
        lastEditMs = System.currentTimeMillis();
        pendingServerQuery = NewerChatConfig.queryServer
                && field.getText().startsWith("/")
                && this.mc.thePlayer != null;
    }

    private void refreshLocal() {
        String text = field.getText();
        int cursor = field.getCursor();
        ChatSyntax.Tok tok = SuggestionProvider.currentToken(text, cursor);
        lastQuery = tok.text;
        lastLocal = SuggestionProvider.build(text, cursor);

        if (!text.startsWith("/") || tok.index != lastServerTokenIndex) {
            lastServer = new ArrayList<Suggestion>();
        }

        menu.set(SuggestionProvider.merge(lastServer, lastLocal, lastQuery), lastQuery);
    }

    private void sendServerQuery() {
        String text = field.getText();
        if (!text.startsWith("/") || this.mc.thePlayer == null) {
            return;
        }
        awaitingToken = SuggestionProvider.currentToken(text, field.getCursor());
        try {
            this.mc.thePlayer.sendQueue.addToSendQueue(new C14PacketTabComplete(text));
        } catch (Throwable ignored) {

        }
    }

    @Override
    public void func_146406_a(String[] results) {
        if (results == null || results.length == 0 || field == null) {
            return;
        }

        String text = field.getText();
        ChatSyntax.Tok tok = awaitingToken != null
                ? awaitingToken
                : SuggestionProvider.currentToken(text, field.getCursor());
        boolean commandName = text.startsWith("/") && tok.index == 0;

        if (commandName) {
            ChatData.learnServerCommands(results);
        }

        List<Suggestion> server = new ArrayList<Suggestion>();
        for (String r : results) {
            if (r == null || r.isEmpty()) {
                continue;
            }
            String insert = (commandName && !r.startsWith("/")) ? "/" + r : r;
            server.add(new Suggestion(insert, null));
        }

        lastServer = server;
        lastServerTokenIndex = tok.index;

        List<Suggestion> merged = SuggestionProvider.merge(server, lastLocal, tok.text);
        if (merged.size() > 200) {
            merged = new ArrayList<Suggestion>(merged.subList(0, 200));
        }
        menu.set(merged, tok.text);
    }

    private void cycleCompletion(int dir) {
        if (!menu.isVisible() || menu.isEmpty()) {
            cycling = false;
            pendingServerQuery = false;
            refreshLocal();
            sendServerQuery();
            return;
        }

        if (!cycling) {
            String text = field.getText();
            ChatSyntax.Tok tok = SuggestionProvider.currentToken(text, field.getCursor());
            cycleBefore = text.substring(0, tok.start);
            cycleAfter = text.substring(tok.end);
            cycleList = new ArrayList<Suggestion>(menu.all());
            cycleIndex = 0;
            cycling = true;
            pendingServerQuery = false;
        } else {
            int n = cycleList.size();
            cycleIndex = ((cycleIndex + dir) % n + n) % n;
        }

        Suggestion s = cycleList.get(cycleIndex);
        String tail = cycleAfter.isEmpty() ? " " : cycleAfter;
        String newText = cycleBefore + s.insert + tail;
        int newCursor = cycleBefore.length() + s.insert.length() + (cycleAfter.isEmpty() ? 1 : 0);
        field.setTextAndCursor(newText, newCursor);
        field.dirty = false;
        menu.setSelectedAbsolute(cycleIndex);
    }

    private void acceptSuggestion(Suggestion suggestion) {
        if (suggestion == null) {
            return;
        }
        String text = field.getText();
        ChatSyntax.Tok tok = SuggestionProvider.currentToken(text, field.getCursor());

        String before = text.substring(0, tok.start);
        String after = text.substring(tok.end);
        String newText = before + suggestion.insert + after;
        int newCursor = before.length() + suggestion.insert.length();

        if (newCursor >= newText.length() || newText.charAt(newCursor) != ' ') {
            newText = newText.substring(0, newCursor) + " " + newText.substring(newCursor);
            newCursor++;
        }

        field.setTextAndCursor(newText, newCursor);
        menu.hide();
        onEdit();
    }

    private void sendMessage(String raw) {
        String msg = raw.trim();
        if (msg.isEmpty()) {
            return;
        }

        this.mc.ingameGUI.getChatGUI().addToSentMessages(msg);
        this.mc.thePlayer.sendChatMessage(msg);
    }

    private void navigateHistory(int dir) {
        List<String> history = getSentMessages();
        int size = history.size();
        int target = clamp(historyIndex + dir, 0, size);
        if (target == historyIndex) {
            return;
        }
        if (target == size) {
            historyIndex = size;
            field.setText(historyStash);
        } else {
            if (historyIndex == size) {
                historyStash = field.getText();
            }
            historyIndex = target;
            field.setText(history.get(target));
        }
        onEdit();
    }

    @SuppressWarnings("unchecked")
    private List<String> getSentMessages() {

        return this.mc.ingameGUI.getChatGUI().getSentMessages();
    }

    private boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
    }

    private URI pendingUrl;

    private boolean handleChatClick(IChatComponent comp) {
        ChatStyle style = comp.getChatStyle();
        if (style == null) {
            return false;
        }
        ClickEvent click = style.getChatClickEvent();
        if (click == null || click.getValue() == null) {
            return false;
        }
        ClickEvent.Action action = click.getAction();
        String value = click.getValue();

        if (action == ClickEvent.Action.OPEN_URL) {
            try {
                URI uri = new URI(value);
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
                if (!scheme.equals("http") && !scheme.equals("https")) {
                    return false;
                }
                this.pendingUrl = uri;
                this.mc.displayGuiScreen(new GuiConfirmOpenLink(this, value, 0, false));
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        if (action == ClickEvent.Action.RUN_COMMAND) {
            this.mc.thePlayer.sendChatMessage(value);
            return true;
        }
        if (action == ClickEvent.Action.SUGGEST_COMMAND) {
            field.setTextAndCursor(value, value.length());
            onEdit();
            return true;
        }
        return false;
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (id == 0) {
            if (result && pendingUrl != null) {
                openUrl(pendingUrl);
            }
            pendingUrl = null;
        }
        this.mc.displayGuiScreen(this);
    }

    private void openUrl(URI uri) {
        try {
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            desktop.getMethod("browse", URI.class).invoke(instance, uri);
        } catch (Throwable primary) {
            try {
                org.lwjgl.Sys.openURL(uri.toString());
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void drawComponentHover(int mouseX, int mouseY) {
        IChatComponent hovered = this.mc.ingameGUI.getChatGUI()
                .func_146236_a(Mouse.getX(), this.mc.displayHeight - Mouse.getY() - 1);
        if (hovered == null) {
            return;
        }
        ChatStyle style = hovered.getChatStyle();
        HoverEvent hover = style == null ? null : style.getChatHoverEvent();
        if (hover == null || hover.getAction() != HoverEvent.Action.SHOW_TEXT || hover.getValue() == null) {
            return;
        }
        String text = hover.getValue().getFormattedText();

        this.func_146283_a(Arrays.asList(text.split("\n")), mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        drawRect(2, this.height - 14, this.width - 2, this.height - 2, NewerChatConfig.barBg);

        field.render(this.width, this.height, cursorCounter);

        ChatSyntax.Tok tok = SuggestionProvider.currentToken(field.getText(), field.getCursor());
        int anchorX = field.screenXOf(tok.start, this.width);
        menu.render(this.width, this.height, mouseX, mouseY, anchorX);

        drawComponentHover(mouseX, mouseY);

    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
