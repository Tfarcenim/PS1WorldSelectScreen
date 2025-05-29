package tfar.ps1worldselectscreen;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PS1WorldSelectScreen.MOD_ID)
public class PS1WorldSelectScreen {
    public static final String MOD_ID = "ps1worldselectscreen";
    // Directly reference a slf4j logger
    static final Logger LOGGER = LogUtils.getLogger();

    static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    public static final String FOLDER = "restarts";

    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC= specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static class Client {

        public final ForgeConfigSpec.BooleanValue regular_world_menu;
        public final ForgeConfigSpec.BooleanValue refresh_preview_image;

        Client(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            regular_world_menu = builder.define("regular_world_menu",false);
            refresh_preview_image = builder.define("refresh_preview_image",true);

            builder.pop();
        }
    }

    public PS1WorldSelectScreen() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
        bus.addListener(this::gather);
        MinecraftForge.EVENT_BUS.addListener(this::screenOpen);
    }

    void screenOpen(ScreenOpenEvent event) {
        Screen screen = event.getScreen();
        if (!CLIENT.regular_world_menu.get() && screen instanceof SelectWorldScreen selectWorldScreen) {
            event.setScreen(new CustomSelectWorldScreen(selectWorldScreen.lastScreen));
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
    }

    private void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(new ModLangProvider(generator));
    }

    static class ModLangProvider extends LanguageProvider {

        public ModLangProvider(DataGenerator gen) {
            super(gen, MOD_ID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add("restartWorld.confirm.title","Confirm Restart");
            add("restartWorld.confirm.description","This will restore the world from a backup and delete all current progress");
            add("selectWorld.restart","Restart");
        }
    }
}
