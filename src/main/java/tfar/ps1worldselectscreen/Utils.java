package tfar.ps1worldselectscreen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class Utils {

    public static File getPreviewFile(LevelSummary summary) {
        String s1 = summary.getIcon().getPath();
        return new File(s1.replace("icon","preview"));
    }

    public static Optional<Path> getPreviewFile(LevelStorageSource.LevelStorageAccess access) {
        return !access.lock.isValid() ? Optional.empty() : Optional.of(access.getWorldDir().resolve("preview.png"));
    }

    public static void takePreviewImage(GameRenderer gameRenderer) {
        IntegratedServer integratedServer = gameRenderer.getMinecraft().getSingleplayerServer();
        if (integratedServer != null) {
            if (gameRenderer.getMinecraft().levelRenderer.countRenderedChunks() > 10 && gameRenderer.getMinecraft().levelRenderer.hasRenderedAllChunks()) {
                getPreviewFile(integratedServer.storageSource).ifPresent(path -> {
                    NativeImage nativeimage = Screenshot.takeScreenshot(gameRenderer.getMinecraft().getMainRenderTarget());
                    Util.ioPool().execute(() -> {
                        int i = nativeimage.getWidth();
                        int j = nativeimage.getHeight();
                        int k = 0;
                        int l = 0;
                        if (i > j) {
                            k = (i - j) / 2;
                            i = j;
                        } else {
                            l = (j - i) / 2;
                            j = i;
                        }

                        try {
                            NativeImage nativeimage1 = new NativeImage(256, 256, false);//note, window is 640 x 480

                            try {
                                nativeimage.resizeSubRectTo(k, l, i, j, nativeimage1);
                                nativeimage1.writeToFile(path);
                            } catch (Throwable throwable1) {
                                try {
                                    nativeimage1.close();
                                } catch (Throwable throwable) {
                                    throwable1.addSuppressed(throwable);
                                }

                                throw throwable1;
                            }

                            nativeimage1.close();
                        } catch (IOException ioexception) {
                            PS1WorldSelectScreen.LOGGER.warn("Couldn't save auto screenshot", ioexception);
                        } finally {
                            nativeimage.close();
                        }
                    });
                });
            }
        }
    }
}
