package tfar.ps1worldselectscreen;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ClientConfig {

    static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public final ForgeConfigSpec.BooleanValue regular_world_menu;
    public final ForgeConfigSpec.BooleanValue refresh_preview_image;
    public final ForgeConfigSpec.BooleanValue show_experimental_warning;

    ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        regular_world_menu = builder.define("regular_world_menu", false);
        refresh_preview_image = builder.define("refresh_preview_image", true);
        show_experimental_warning = builder.define("show_experimental_warning", false);

        builder.pop();
    }
}
