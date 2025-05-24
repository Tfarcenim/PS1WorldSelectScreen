package tfar.ps1worldselectscreen;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelSummary;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class SubWorldScreen extends Screen implements AutoCloseable {
    private final CustomSelectWorldScreen previous;
    private final LevelSummary summary;
    private final ResourceLocation iconLocation;

    private Button selectButton;
    private Button restartButton;

    @Nullable
    private File iconFile;
    @Nullable
    private DynamicTexture icon;
    protected SubWorldScreen(Component pTitle, CustomSelectWorldScreen previous, LevelSummary summary, TwoColumnSelectionList.WorldListEntry entry) {
        super(pTitle);
        this.previous = previous;
        this.summary = summary;
        String s = summary.getLevelId();
        this.iconLocation = new ResourceLocation("minecraft", "worlds/" + Util.sanitizeName(s, ResourceLocation::validPathChar) + "/" +
                Hashing.sha1().hashUnencodedChars(s) + "/preview");
        String s1 = summary.getIcon().getPath();
        this.iconFile = new File(s1.replace("icon","preview"));
        if (!this.iconFile.isFile()) {
            this.iconFile = null;
        }

    }

    @Override
    protected void init() {
        super.init();
        this.icon = this.loadIcon();
        this.selectButton = this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 52, 150, 20, new TranslatableComponent("selectWorld.select"), (p_101378_) -> {
            previous.list.getSelectedOpt().ifPresent(TwoColumnSelectionList.WorldListEntry::joinWorld);
        }));

        this.restartButton = this.addRenderableWidget(new Button(this.width / 2 + 14, this.height - 52, 150, 20, new TranslatableComponent("selectWorld.restart"), (p_101378_) -> {
            //todo previous.list.getSelectedOpt().ifPresent(t -> t.joinWorld());
        }));
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 20, -1);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ResourceLocation texture =  this.icon != null ? this.iconLocation : TwoColumnSelectionList.ICON_MISSING;

        int iconx = icon != null ? icon.getPixels().getWidth() : 64;
        int icony = icon != null ? icon.getPixels().getHeight() : 64;

        RenderSystem.setShaderTexture(0,texture);
        RenderSystem.enableBlend();
        GuiComponent.blit(pPoseStack, width/2-iconx/4, height/2-icony/4-16, 0.0F, 0.0F, iconx/2, icony/2, iconx/2, icony/2);
        RenderSystem.disableBlend();

    }

    @Nullable
    private DynamicTexture loadIcon() {
        boolean flag = this.iconFile != null && this.iconFile.isFile();
        if (flag) {
            try {
                InputStream inputstream = new FileInputStream(this.iconFile);

                DynamicTexture dynamictexture1;
                try {
                    NativeImage nativeimage = NativeImage.read(inputstream);
                    //Validate.validState(nativeimage.getWidth() == 64, "Must be 64 pixels wide");
                    //Validate.validState(nativeimage.getHeight() == 64, "Must be 64 pixels high");
                    DynamicTexture dynamictexture = new DynamicTexture(nativeimage);
                    this.minecraft.getTextureManager().register(this.iconLocation, dynamictexture);
                    dynamictexture1 = dynamictexture;
                } catch (Throwable throwable1) {
                    try {
                        inputstream.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }

                    throw throwable1;
                }

                inputstream.close();
                return dynamictexture1;
            } catch (Throwable throwable2) {
                TwoColumnSelectionList.LOGGER.error("Invalid icon for world {}", this.summary.getLevelId(), throwable2);
                this.iconFile = null;
                return null;
            }
        } else {
            this.minecraft.getTextureManager().release(this.iconLocation);
            return null;
        }
    }

    @Override
    public void close() {
        if (this.icon != null) {
            this.icon.close();
        }

    }

    @Override
    public void onClose() {
        popScreen();
    }

    public void popScreen() {
        this.minecraft.setScreen(this.previous);
    }

}
