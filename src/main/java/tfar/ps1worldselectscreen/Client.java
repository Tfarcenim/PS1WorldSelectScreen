package tfar.ps1worldselectscreen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraftforge.client.event.ScreenOpenEvent;

import java.util.ArrayList;
import java.util.List;

public class Client {
    public static final List<String> LOCKED_WORLDS = new ArrayList<>();


    static {
        LOCKED_WORLDS.add("1977-09-26");
        LOCKED_WORLDS.add("1979-06-01");
        LOCKED_WORLDS.add("1983-12-26");
        LOCKED_WORLDS.add("1986-06-03");
        LOCKED_WORLDS.add("1986-08-22");
        LOCKED_WORLDS.add("1995-10-31");

        LOCKED_WORLDS.add("1996-06-19");
        LOCKED_WORLDS.add("1997-07-07");

        LOCKED_WORLDS.add("1998-03-12");
        LOCKED_WORLDS.add("1998-10-13");
        LOCKED_WORLDS.add("1999-05-16");
    }
    
    static void screenOpen(ScreenOpenEvent event) {
        Screen screen = event.getScreen();
        if (!ClientConfig.CLIENT.regular_world_menu.get() && screen instanceof SelectWorldScreen selectWorldScreen) {
            event.setScreen(new CustomSelectWorldScreen(selectWorldScreen.lastScreen));
        }
    }
}
