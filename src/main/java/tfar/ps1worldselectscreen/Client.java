package tfar.ps1worldselectscreen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraftforge.client.event.ScreenOpenEvent;

public class Client {
    static void screenOpen(ScreenOpenEvent event) {
        Screen screen = event.getScreen();
        if (!ClientConfig.CLIENT.regular_world_menu.get() && screen instanceof SelectWorldScreen selectWorldScreen) {
            event.setScreen(new CustomSelectWorldScreen(selectWorldScreen.lastScreen));
        }
    }
}
