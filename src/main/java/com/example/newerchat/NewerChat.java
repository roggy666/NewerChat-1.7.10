package com.example.newerchat;

import com.example.newerchat.client.ChatGuiHandler;
import com.example.newerchat.config.NewerChatConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(
        modid = NewerChat.MODID,
        name = "NewerChat",
        version = "1.0.0",
        acceptedMinecraftVersions = "[1.7.10]"
)
public class NewerChat {

    public static final String MODID = "newerchat";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NewerChatConfig.load(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        MinecraftForge.EVENT_BUS.register(new ChatGuiHandler());

        FMLCommonHandler.instance().bus().register(new NewerChatConfig());
    }
}
