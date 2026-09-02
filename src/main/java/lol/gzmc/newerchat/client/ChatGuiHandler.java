package lol.gzmc.newerchat.client;

import lol.gzmc.newerchat.client.gui.GuiChatNew;
import lol.gzmc.newerchat.config.NewerChatConfig;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiOpenEvent;

public class ChatGuiHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!NewerChatConfig.enabled) {
            return;
        }
        if (event.gui == null || event.gui.getClass() != GuiChat.class) {
            return;
        }

        String defaultText = readDefaultText((GuiChat) event.gui);
        event.gui = new GuiChatNew(defaultText);
    }

    private static String readDefaultText(GuiChat gui) {
        try {
            String v = ReflectionHelper.getPrivateValue(
                    GuiChat.class, gui, "field_146409_v", "defaultInputFieldText");
            return v == null ? "" : v;
        } catch (Throwable t) {
            return "";
        }
    }
}
