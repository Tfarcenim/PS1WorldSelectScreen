package tfar.ps1worldselectscreen;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.WorldStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class TwoColumnSelectionList extends ObjectSelectionList<TwoColumnSelectionList.WorldListEntry> {
    static final Logger LOGGER = LogUtils.getLogger();
    static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
    static final ResourceLocation ICON_OVERLAY_LOCATION = new ResourceLocation("textures/gui/world_selection.png");
    private static final ResourceLocation FORGE_EXPERIMENTAL_WARNING_ICON = new ResourceLocation("forge","textures/gui/experimental_warning.png");
    static final Component FROM_NEWER_TOOLTIP_1 = new TranslatableComponent("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
    static final Component FROM_NEWER_TOOLTIP_2 = new TranslatableComponent("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
    static final Component SNAPSHOT_TOOLTIP_1 = new TranslatableComponent("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
    static final Component SNAPSHOT_TOOLTIP_2 = new TranslatableComponent("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
    static final Component WORLD_LOCKED_TOOLTIP = new TranslatableComponent("selectWorld.locked").withStyle(ChatFormatting.RED);
    static final Component WORLD_REQUIRES_CONVERSION = new TranslatableComponent("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
    private final CustomSelectWorldScreen screen;
    @Nullable
    private List<LevelSummary> cachedList;

    public TwoColumnSelectionList(CustomSelectWorldScreen pScreen, Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight, Supplier<String> pFilterLevelSupplier, @Nullable TwoColumnSelectionList pTwoColumnSelectionList) {
        super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
        this.screen = pScreen;
        if (pTwoColumnSelectionList != null) {
            this.cachedList = pTwoColumnSelectionList.cachedList;
        }

       // if (PS1WorldSelectScreen.CLIENT.hideBottomBar.get()) {
      //      setRenderTopAndBottom(false);
       // }

        this.refreshList(pFilterLevelSupplier, false);
    }

    public void refreshList(Supplier<String> pFilterLevelSupplier, boolean pUpdateCache) {
        this.clearEntries();
        LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
        if (this.cachedList == null || pUpdateCache) {
            try {
                this.cachedList = levelstoragesource.getLevelList();
            } catch (LevelStorageException levelstorageexception) {
                LOGGER.error("Couldn't load level list", levelstorageexception);
                this.minecraft.setScreen(new ErrorScreen(new TranslatableComponent("selectWorld.unable_to_load"), new TextComponent(levelstorageexception.getMessage())));
                return;
            }

            Collections.sort(this.cachedList);
        }

        if (this.cachedList.isEmpty()) {
            this.minecraft.setScreen(CreateWorldScreen.createFresh(null));
        } else {
            String s = pFilterLevelSupplier.get().toLowerCase(Locale.ROOT);

            List<LevelSummary> list = this.cachedList;

            int columns = 2;
            int rows = (int) Math.ceil((double) list.size() / columns);

            for (int r = 0; r < rows; r++) {
                int index = r * columns;
                LevelSummary levelsummary = list.get(index);

                LevelSummary levelSummary1 = index + 1 < list.size() ? list.get(index + 1) : null;

                if (levelsummary.getLevelName().toLowerCase(Locale.ROOT).contains(s) || levelsummary.getLevelId().toLowerCase(Locale.ROOT).contains(s)) {
                    this.addEntry(new WorldListEntry(this, levelsummary, levelSummary1));
                }
            }
        }
    }

    protected int getScrollbarPosition() {
        return width-5;
    }

    public int getRowWidth() {
        return width - 32;
    }

    protected boolean isFocused() {
        return this.screen.getFocused() == this;
    }

    public void setSelected(@Nullable TwoColumnSelectionList.WorldListEntry pEntry) {
        super.setSelected(pEntry);
     //   this.screen.updateButtonStatus(pEntry != null && !pEntry.summary0.isDisabled());
    }

    protected void moveSelection(AbstractSelectionList.SelectionDirection pOrdering) {
        this.moveSelection(pOrdering, p_101681_ -> {
            return !p_101681_.summary0.isDisabled();
        });
    }

    public Optional<TwoColumnSelectionList.WorldListEntry> getSelectedOpt() {
        return Optional.ofNullable(this.getSelected());
    }

    public CustomSelectWorldScreen getScreen() {
        return this.screen;
    }

    public final class WorldListEntry extends ObjectSelectionList.Entry<TwoColumnSelectionList.WorldListEntry> implements AutoCloseable {
        private static final int ICON_WIDTH = 32;
        private static final int ICON_HEIGHT = 32;
        private static final int ICON_OVERLAY_X_JOIN = 0;
        private static final int ICON_OVERLAY_X_JOIN_WITH_NOTIFY = 32;
        private static final int ICON_OVERLAY_X_WARNING = 64;
        private static final int ICON_OVERLAY_X_ERROR = 96;
        private static final int ICON_OVERLAY_Y_UNSELECTED = 0;
        private static final int ICON_OVERLAY_Y_SELECTED = 32;
        private final Minecraft minecraft;
        private final CustomSelectWorldScreen screen;

        /////////
        final LevelSummary summary0;

        private final ResourceLocation iconLocation0;
        @Nullable
        private File iconFile0;
        @Nullable
        private final DynamicTexture icon0;

        ////////
        @Nullable final LevelSummary summary1;

        private final ResourceLocation iconLocation1;
        @Nullable
        private File iconFile1;
        @Nullable
        private final DynamicTexture icon1;

        private long lastClickTime;

        public WorldListEntry(TwoColumnSelectionList pTwoColumnSelectionList, LevelSummary pSummary,@Nullable LevelSummary summary1) {
            this.screen = pTwoColumnSelectionList.getScreen();
            this.summary0 = pSummary;
            this.minecraft = Minecraft.getInstance();
            String levelid0 = pSummary.getLevelId();
            this.iconLocation0 = new ResourceLocation("minecraft", "worlds/" + Util.sanitizeName(levelid0, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(levelid0) + "/icon");
            this.iconFile0 = Utils.getPreviewFile(pSummary);
            if (!this.iconFile0.isFile()) {
                this.iconFile0 = null;
            }
            this.icon0 = this.loadServerIcon(true);

            this.summary1 = summary1;
            if (summary1 != null) {
                String levelid1 = summary1.getLevelId();
                this.iconLocation1 = new ResourceLocation("minecraft", "worlds/" + Util.sanitizeName(levelid1, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(levelid0) + "/icon");
                this.iconFile1 = Utils.getPreviewFile(summary1);
                if (!this.iconFile1.isFile()) {
                    this.iconFile1 = null;
                }
                this.icon1 = this.loadServerIcon(false);
            } else {
                iconLocation1 = null;
                icon1 = null;
                iconFile1 = null;
            }

        }

        public Component getNarration() {
            TranslatableComponent translatablecomponent = new TranslatableComponent("narrator.select.world", this.summary0.getLevelName(), new Date(this.summary0.getLastPlayed()), this.summary0.isHardcore() ? new TranslatableComponent("gameMode.hardcore") : new TranslatableComponent("gameMode." + this.summary0.getGameMode().getName()), this.summary0.hasCheats() ? new TranslatableComponent("selectWorld.cheats") : TextComponent.EMPTY, this.summary0.getWorldVersionName());
            Component component;
            if (this.summary0.isLocked()) {
                component = CommonComponents.joinForNarration(translatablecomponent, TwoColumnSelectionList.WORLD_LOCKED_TOOLTIP);
            } else {
                component = translatablecomponent;
            }

            return new TranslatableComponent("narrator.select", component);
        }

        public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            renderPanel(pPoseStack, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, pIsMouseOver, pPartialTick,true);
            renderPanel(pPoseStack, pIndex, pTop, pLeft+ width/2, pWidth, pHeight, pMouseX, pMouseY, pIsMouseOver, pPartialTick,false);
        }

        public void renderPanel(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick
        ,boolean left) {
            LevelSummary summary = left ? summary0 : summary1;
            if (summary == null) return;

            DynamicTexture icon = left ? icon0 : icon1;
            ResourceLocation iconLocation = left ? iconLocation0 : iconLocation1;
            String s = summary.getLevelName();
            // String s1 = this.summary0.getLevelId() + " (" + TwoColumnSelectionList.DATE_FORMAT.format(new Date(this.summary0.getLastPlayed())) + ")";
            if (StringUtils.isEmpty(s)) {
                s = I18n.get("selectWorld.world") + " " + (pIndex + 1);
            }

            //   Component component = this.summary0.getInfo();

            Component c = new TextComponent(s);




            List<FormattedText> formattedText = Utils.findOptimalLines(c, 100);

            for (int i = 0; i < formattedText.size(); i++) {
                FormattedText text = formattedText.get(i);
                this.minecraft.font.draw(pPoseStack, text.getString(), pLeft + 64 + 3, pTop + 1 + i * minecraft.font.lineHeight, 0xffffff);
            }

            // this.minecraft.font.draw(pPoseStack, s1, (float)(pLeft + 32 + 3), (float)(pTop + 9 + 3), 0x808080);
            // this.minecraft.font.draw(pPoseStack, component, (float)(pLeft + 32 + 3), (float)(pTop + 9 + 9 + 3), 0x808080);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, icon != null ? iconLocation : TwoColumnSelectionList.ICON_MISSING);
            RenderSystem.enableBlend();
            GuiComponent.blit(pPoseStack, pLeft, pTop, 0.0F, 0.0F, 64, 64, 64, 64);
            RenderSystem.disableBlend();
            renderExperimentalWarning(pPoseStack, pMouseX, pMouseY, pTop, pLeft,summary);
           /* if (this.minecraft.options.touchscreen || pIsMouseOver) {
                RenderSystem.setShaderTexture(0, TwoColumnSelectionList.ICON_OVERLAY_LOCATION);
                GuiComponent.fill(pPoseStack, pLeft, pTop, pLeft + 32, pTop + 32, 0xa0909090);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                int i = pMouseX - pLeft;
                boolean flag = i < 32;
                int j = flag ? 32 : 0;
                if (summary.isLocked()) {
                    GuiComponent.blit(pPoseStack, pLeft, pTop, 96.0F, (float)j, 32, 32, 256, 256);
                    if (flag) {
                        this.screen.setToolTip(this.minecraft.font.split(TwoColumnSelectionList.WORLD_LOCKED_TOOLTIP, 175));
                    }
                } else if (summary.requiresManualConversion()) {
                    GuiComponent.blit(pPoseStack, pLeft, pTop, 96.0F, (float)j, 32, 32, 256, 256);
                    if (flag) {
                        this.screen.setToolTip(this.minecraft.font.split(TwoColumnSelectionList.WORLD_REQUIRES_CONVERSION, 175));
                    }
                } else if (summary.markVersionInList()) {
                    GuiComponent.blit(pPoseStack, pLeft, pTop, 32.0F, (float)j, 32, 32, 256, 256);
                    if (summary.askToOpenWorld()) {
                        GuiComponent.blit(pPoseStack, pLeft, pTop, 96.0F, (float)j, 32, 32, 256, 256);
                        if (flag) {
                            this.screen.setToolTip(ImmutableList.of(TwoColumnSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(), TwoColumnSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()));
                        }
                    } else if (!SharedConstants.getCurrentVersion().isStable()) {
                        GuiComponent.blit(pPoseStack, pLeft, pTop, 64.0F, (float)j, 32, 32, 256, 256);
                        if (flag) {
                            this.screen.setToolTip(ImmutableList.of(TwoColumnSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), TwoColumnSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()));
                        }
                    }
                } else {
                    GuiComponent.blit(pPoseStack, pLeft, pTop, 0.0F, (float)j, 32, 32, 256, 256);
                }
            }*/
        }

        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            boolean left = pMouseX < width / 2d;
            LevelSummary summary = left ? summary0 : summary1;
            if (summary == null || summary.isDisabled()) {
                return true;
            } else {
                minecraft.setScreen(new SubWorldScreen(new TextComponent(summary.getLevelName()), this.screen, summary));
                return true;
            }
        }

        public void deleteWorld() {
            this.minecraft.setScreen(new ConfirmScreen(p_170322_ -> {
                if (p_170322_) {
                    this.minecraft.setScreen(new ProgressScreen(true));
                    this.doDeleteWorld();
                }

                this.minecraft.setScreen(this.screen);
            }, new TranslatableComponent("selectWorld.deleteQuestion"), new TranslatableComponent("selectWorld.deleteWarning", this.summary0.getLevelName()), new TranslatableComponent("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
        }

        public void doDeleteWorld() {
            LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
            String s = this.summary0.getLevelId();

            try {
                LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.createAccess(s);

                try {
                    levelstoragesource$levelstorageaccess.deleteLevel();
                } catch (Throwable throwable1) {
                    if (levelstoragesource$levelstorageaccess != null) {
                        try {
                            levelstoragesource$levelstorageaccess.close();
                        } catch (Throwable throwable) {
                            throwable1.addSuppressed(throwable);
                        }
                    }

                    throw throwable1;
                }

                if (levelstoragesource$levelstorageaccess != null) {
                    levelstoragesource$levelstorageaccess.close();
                }
            } catch (IOException ioexception) {
                SystemToast.onWorldDeleteFailure(this.minecraft, s);
                TwoColumnSelectionList.LOGGER.error("Failed to delete world {}", s, ioexception);
            }

            TwoColumnSelectionList.this.refreshList(() -> {
                return this.screen.searchBox.getValue();
            }, true);
        }

        public void editWorld() {
            String s = this.summary0.getLevelId();

            try {
                LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().createAccess(s);
                this.minecraft.setScreen(new EditWorldScreen(p_101719_ -> {
                    try {
                        levelstoragesource$levelstorageaccess.close();
                    } catch (IOException ioexception1) {
                        TwoColumnSelectionList.LOGGER.error("Failed to unlock level {}", s, ioexception1);
                    }

                    if (p_101719_) {
                        TwoColumnSelectionList.this.refreshList(() -> {
                            return this.screen.searchBox.getValue();
                        }, true);
                    }

                    this.minecraft.setScreen(this.screen);
                }, levelstoragesource$levelstorageaccess));
            } catch (IOException ioexception) {
                SystemToast.onWorldAccessFailure(this.minecraft, s);
                TwoColumnSelectionList.LOGGER.error("Failed to access level {}", s, ioexception);
                TwoColumnSelectionList.this.refreshList(() -> {
                    return this.screen.searchBox.getValue();
                }, true);
            }

        }

        public void recreateWorld() {
            this.queueLoadScreen();

            try {
                LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().createAccess(this.summary0.getLevelId());

                try {
                    WorldStem worldstem = this.minecraft.makeWorldStem(levelstoragesource$levelstorageaccess, false);

                    try {
                        WorldGenSettings worldgensettings = worldstem.worldData().worldGenSettings();
                        Path path = CreateWorldScreen.createTempDataPackDirFromExistingWorld(levelstoragesource$levelstorageaccess.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft);
                        if (worldgensettings.isOldCustomizedWorld()) {
                            this.minecraft.setScreen(new ConfirmScreen(p_205503_ -> {
                                this.minecraft.setScreen(p_205503_ ? CreateWorldScreen.createFromExisting(this.screen, worldstem, path) : this.screen);
                            }, new TranslatableComponent("selectWorld.recreate.customized.title"), new TranslatableComponent("selectWorld.recreate.customized.text"), CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
                        } else {
                            this.minecraft.setScreen(CreateWorldScreen.createFromExisting(this.screen, worldstem, path));
                        }
                    } catch (Throwable throwable2) {
                        if (worldstem != null) {
                            try {
                                worldstem.close();
                            } catch (Throwable throwable1) {
                                throwable2.addSuppressed(throwable1);
                            }
                        }

                        throw throwable2;
                    }

                    if (worldstem != null) {
                        worldstem.close();
                    }
                } catch (Throwable throwable3) {
                    if (levelstoragesource$levelstorageaccess != null) {
                        try {
                            levelstoragesource$levelstorageaccess.close();
                        } catch (Throwable throwable) {
                            throwable3.addSuppressed(throwable);
                        }
                    }

                    throw throwable3;
                }

                if (levelstoragesource$levelstorageaccess != null) {
                    levelstoragesource$levelstorageaccess.close();
                }
            } catch (Exception exception) {
                TwoColumnSelectionList.LOGGER.error("Unable to recreate world", exception);
                this.minecraft.setScreen(new AlertScreen(() -> {
                    this.minecraft.setScreen(this.screen);
                }, new TranslatableComponent("selectWorld.recreate.error.title"), new TranslatableComponent("selectWorld.recreate.error.text")));
            }

        }

        private void queueLoadScreen() {
            this.minecraft.forceSetScreen(new GenericDirtMessageScreen(new TranslatableComponent("selectWorld.data_read")));
        }

        @Nullable
        private DynamicTexture loadServerIcon(boolean left) {
            File iconFile = left ? iconFile0 : iconFile1;
            ResourceLocation iconLocation = left ? iconLocation0 : iconLocation1;
            boolean flag = iconFile != null && iconFile.isFile();
            if (flag) {
                try {
                    InputStream inputstream = new FileInputStream(iconFile);

                    DynamicTexture dynamictexture1;
                    try {
                        NativeImage nativeimage = NativeImage.read(inputstream);
                     //   Validate.validState(nativeimage.getWidth() == 64, "Must be 64 pixels wide");
                     //   Validate.validState(nativeimage.getHeight() == 64, "Must be 64 pixels high");
                        DynamicTexture dynamictexture = new DynamicTexture(nativeimage);
                        this.minecraft.getTextureManager().register(iconLocation, dynamictexture);
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
                    TwoColumnSelectionList.LOGGER.error("Invalid icon for world {}",left?  this.summary0.getLevelId() :  this.summary1.getLevelId(), throwable2);
                    if (left) {
                        iconFile0 = null;
                    } else {
                        iconFile1 = null;
                    }
                    return null;
                }
            } else {
                this.minecraft.getTextureManager().release(this.iconLocation0);
                return null;
            }
        }

        public void close() {
            if (this.icon0 != null) {
                this.icon0.close();
            }
            if (this.icon1 != null) {
                this.icon1.close();
            }
        }

        public String getLevelName() {
            return this.summary0.getLevelName();
        }
        private void renderExperimentalWarning(PoseStack stack, int mouseX, int mouseY, int top, int left, LevelSummary summary) {
            if (summary.isExperimental() && PS1WorldSelectScreen.CLIENT.show_experimental_warning.get()) {
                int leftStart = left +100;
                RenderSystem.setShaderTexture(0, TwoColumnSelectionList.FORGE_EXPERIMENTAL_WARNING_ICON);
                GuiComponent.blit(stack, leftStart - 36, top+8, 0.0F, 0.0F, 32, 32, 32, 32);
                //Reset texture to what it was before
                RenderSystem.setShaderTexture(0, this.icon0 != null ? this.iconLocation0 : TwoColumnSelectionList.ICON_MISSING);
                if (TwoColumnSelectionList.this.getEntryAtPosition(mouseX, mouseY) == this && mouseX > leftStart - 36 && mouseX < leftStart) {
                    List<net.minecraft.util.FormattedCharSequence> tooltip = Minecraft.getInstance().font.split(new TranslatableComponent("forge.experimentalsettings.tooltip"), 200);
                    TwoColumnSelectionList.this.screen.renderTooltip(stack, tooltip, mouseX, mouseY);
                }
            }
        }
    }
}
