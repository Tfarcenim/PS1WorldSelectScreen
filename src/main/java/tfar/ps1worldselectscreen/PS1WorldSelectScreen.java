package tfar.ps1worldselectscreen;

import com.mojang.logging.LogUtils;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(PS1WorldSelectScreen.MOD_ID)
public class PS1WorldSelectScreen {
    public static final String MOD_ID = "ps1worldselectscreen";
    // Directly reference a slf4j logger
    static final Logger LOGGER = LogUtils.getLogger();

    public static final String FOLDER = "restarts";


    public PS1WorldSelectScreen() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::gather);
        if (FMLEnvironment.dist.isClient()) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC);
            MinecraftForge.EVENT_BUS.addListener(Client::screenOpen);
        }
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
            add("restartWorld.missing","There are no backup worlds!");
            add("restartWorld.missing.back","Go back to previous screen");
        }
    }
}
