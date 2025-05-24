package tfar.ps1worldselectscreen;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PS1WorldSelectScreen.MOD_ID)
public class PS1WorldSelectScreen
{
    public static final String MOD_ID = "ps1worldselectscreen";
    // Directly reference a slf4j logger
    static final Logger LOGGER = LogUtils.getLogger();

    public PS1WorldSelectScreen() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
        bus.addListener(this::gather);
        MinecraftForge.EVENT_BUS.addListener(this::screenOpen);
    }

    void screenOpen(ScreenOpenEvent event) {
        Screen screen = event.getScreen();
        if (screen instanceof SelectWorldScreen selectWorldScreen) {
            event.setScreen(new CustomSelectWorldScreen(selectWorldScreen.lastScreen));
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void gather(GatherDataEvent event) {

    }
}
