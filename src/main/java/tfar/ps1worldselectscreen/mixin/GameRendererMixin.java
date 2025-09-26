package tfar.ps1worldselectscreen.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.ps1worldselectscreen.ClientConfig;
import tfar.ps1worldselectscreen.PS1WorldSelectScreen;
import tfar.ps1worldselectscreen.Utils;

import java.nio.file.Path;
import java.util.Optional;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Shadow private boolean hasWorldScreenshot;
	@Unique
	private boolean hasScreenshot;

	@Redirect(
			method = {"tryTakeScreenshotIfNeeded"},
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/renderer/GameRenderer;hasWorldScreenshot:Z"
			)
	)
	private boolean on_tryTakeScreenshotIfNeeded_hasWorldScreenshot(GameRenderer instance) {
		return this.hasScreenshot && ClientConfig.CLIENT.refresh_preview_image.get() || this.hasWorldScreenshot;
	}


	@Inject(method = "tryTakeScreenshotIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;getWorldScreenshotFile()Ljava/util/Optional;"))
	private void takePreviewImage(CallbackInfo ci) {
		Utils.takePreviewImage((GameRenderer)(Object) this);
	}

	@Redirect(
			method = {"tryTakeScreenshotIfNeeded"},
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/server/IntegratedServer;getWorldScreenshotFile()Ljava/util/Optional;"
			)
	)
	private Optional<Path> on_tryTakeScreenshotIfNeeded_isRegularFile(IntegratedServer instance) {
		instance.getWorldScreenshotFile().ifPresent(this::takeAutoScreenshot);
		return Optional.empty();
	}


	@Shadow protected abstract void takeAutoScreenshot(Path pPath);

	@Inject(
			method = {"takeAutoScreenshot"},
			at = {@At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/Screenshot;takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lcom/mojang/blaze3d/platform/NativeImage;"
			)}
	)
	private void on_takeAutoScreenshot(Path $$0, CallbackInfo ci) {
		this.hasScreenshot = true;
	}

	@Inject(
			method = {"resetData"},
			at = {@At("HEAD")}
	)
	private void on_resetData(CallbackInfo ci) {
		this.hasScreenshot = false;
	}

}
