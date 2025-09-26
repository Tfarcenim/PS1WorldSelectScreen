package tfar.ps1worldselectscreen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public class SubWorldScreen extends Screen implements AutoCloseable {
    private final CustomSelectWorldScreen previous;
    private final LevelSummary summary;
    private final ResourceLocation previewLocation;

    private Button selectButton;
    private Button restartButton;

    @Nullable
    private DynamicTexture preview;
    protected SubWorldScreen(Component pTitle, CustomSelectWorldScreen previous, LevelSummary summary, ResourceLocation previewLocation,
                             @Nullable DynamicTexture texture) {
        super(pTitle);
        this.previous = previous;
        this.summary = summary;
        this.previewLocation = previewLocation;
        this.preview = texture;
    }

    @Override
    protected void init() {
        super.init();

        int xCenter = width/2;

        int buttonWidth = 120;

        int spacing = 32;

        this.selectButton = this.addRenderableWidget(new Button(xCenter - buttonWidth - spacing/2, this.height - 52, buttonWidth, 20,
                new TranslatableComponent("selectWorld.select"), b -> joinWorld()));

        this.restartButton = this.addRenderableWidget(new Button(xCenter+ spacing/2, this.height - 52, buttonWidth, 20, new TranslatableComponent("selectWorld.restart"), p_101378_ -> {
            try (LevelStorageSource.LevelStorageAccess access = this.minecraft.getLevelSource().createAccess(summary.getLevelId())) {
                minecraft.setScreen(new ConfirmScreen(confirmed -> {
                            if (confirmed) {
                                doDeleteWorld();
                                loadBackupAndShowToast();
                                joinWorld();
                            } else {
                                minecraft.setScreen(this);
                            }
                        }, new TranslatableComponent("restartWorld.confirm.title"), new TranslatableComponent("restartWorld.confirm.description"))
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }


    public void loadBackupAndShowToast() {
        LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
        Path saveDir = levelstoragesource.getBaseDir();
        String levelId = summary.getLevelId();
        Path levelPath = saveDir.resolve(levelId);

        Path restartDir = minecraft.gameDirectory.toPath().resolve(PS1WorldSelectScreen.FOLDER);
        Path restartPath = restartDir.resolve(levelId);

        try {
            FileUtils.copyDirectory(restartPath.toFile(),levelPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void doDeleteWorld() {
        LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
        String s = this.summary.getLevelId();

        try {
            LevelStorageSource.LevelStorageAccess storageAccess = levelstoragesource.createAccess(s);

            try {
                storageAccess.deleteLevel();
            } catch (Throwable throwable1) {
                if (storageAccess != null) {
                    try {
                        storageAccess.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }
                }

                throw throwable1;
            }

            if (storageAccess != null) {
                storageAccess.close();
            }
        } catch (IOException ioexception) {
            SystemToast.onWorldDeleteFailure(this.minecraft, s);
            TwoColumnSelectionList.LOGGER.error("Failed to delete world {}", s, ioexception);
        }

       // previous.list.refreshList(() -> {
       //     return this.previous.searchBox.getValue();
       // }, true);
    }


    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pPoseStack);
        drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 20, -1);
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ResourceLocation texture =  this.preview != null ? this.previewLocation : TwoColumnSelectionList.ICON_MISSING;

        int iconx = preview != null ? preview.getPixels().getWidth() : 128;
        int icony = preview != null ? preview.getPixels().getHeight() : 128;

        RenderSystem.setShaderTexture(0,texture);
        RenderSystem.enableBlend();
        GuiComponent.blit(pPoseStack, width/2-iconx/4, height/2-icony/4-24,
                0, 0, iconx/2, icony/2, iconx/2, icony/2);
        RenderSystem.disableBlend();

    }

    public void joinWorld() {
        if (!this.summary.isDisabled()) {
            LevelSummary.BackupStatus levelsummary$backupstatus = this.summary.backupStatus();
            if (levelsummary$backupstatus.shouldBackup()) {
                String s = "selectWorld.backupQuestion." + levelsummary$backupstatus.getTranslationKey();
                String s1 = "selectWorld.backupWarning." + levelsummary$backupstatus.getTranslationKey();
                MutableComponent mutablecomponent = new TranslatableComponent(s);
                if (levelsummary$backupstatus.isSevere()) {
                    mutablecomponent.withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
                }

                Component component = new TranslatableComponent(s1, this.summary.getWorldVersionName(), SharedConstants.getCurrentVersion().getName());
                this.minecraft.setScreen(new BackupConfirmScreen(this, (p_101736_, p_101737_) -> {
                    if (p_101736_) {
                        String s2 = this.summary.getLevelId();

                        try {
                            LevelStorageSource.LevelStorageAccess levelStorageAccess = this.minecraft.getLevelSource().createAccess(s2);

                            try {
                                EditWorldScreen.makeBackupAndShowToast(levelStorageAccess);
                            } catch (Throwable throwable1) {
                                if (levelStorageAccess != null) {
                                    try {
                                        levelStorageAccess.close();
                                    } catch (Throwable throwable) {
                                        throwable1.addSuppressed(throwable);
                                    }
                                }

                                throw throwable1;
                            }

                            if (levelStorageAccess != null) {
                                levelStorageAccess.close();
                            }
                        } catch (IOException ioexception) {
                            SystemToast.onWorldAccessFailure(this.minecraft, s2);
                            TwoColumnSelectionList.LOGGER.error("Failed to backup level {}", s2, ioexception);
                        }
                    }

                    this.loadWorld();
                }, mutablecomponent, component, false));
            } else if (this.summary.askToOpenWorld()) {
                this.minecraft.setScreen(new ConfirmScreen(p_101741_ -> {
                    if (p_101741_) {
                        try {
                            this.loadWorld();
                        } catch (Exception exception) {
                            TwoColumnSelectionList.LOGGER.error("Failure to open 'future world'", exception);
                            this.minecraft.setScreen(new AlertScreen(() -> {
                                this.minecraft.setScreen(this);
                            }, new TranslatableComponent("selectWorld.futureworld.error.title"),
                                    new TranslatableComponent("selectWorld.futureworld.error.text")));
                        }
                    } else {
                        this.minecraft.setScreen(this);
                    }

                }, new TranslatableComponent("selectWorld.versionQuestion"),
                        new TranslatableComponent("selectWorld.versionWarning", this.summary.getWorldVersionName()),
                        new TranslatableComponent("selectWorld.versionJoinButton"), CommonComponents.GUI_CANCEL));
            } else {
                this.loadWorld();
            }

        }
    }

    private void loadWorld() {
        if (this.minecraft.getLevelSource().levelExists(this.summary.getLevelId())) {
            this.queueLoadScreen();
            this.minecraft.loadLevel(this.summary.getLevelId());
        }
    }

    private void queueLoadScreen() {
        this.minecraft.forceSetScreen(new GenericDirtMessageScreen(new TranslatableComponent("selectWorld.data_read")));
    }


    @Override
    public void close() {
        if (this.preview != null) {
            this.preview.close();
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
