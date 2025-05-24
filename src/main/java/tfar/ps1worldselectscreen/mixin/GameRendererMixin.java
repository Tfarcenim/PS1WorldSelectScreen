package tfar.ps1worldselectscreen.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.ps1worldselectscreen.Utils;

import java.nio.file.Path;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "tryTakeScreenshotIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;getWorldScreenshotFile()Ljava/util/Optional;"))
	private void takePreviewImage(CallbackInfo ci) {
		Utils.takePreviewImage((GameRenderer)(Object) this);
	}
}
