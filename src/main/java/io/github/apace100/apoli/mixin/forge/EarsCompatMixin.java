package io.github.apace100.apoli.mixin.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unascribed.ears.common.render.IndirectEarsRenderDelegate;
import io.github.edwinmindcraft.apoli.common.power.configuration.ColorConfiguration;
import io.github.edwinmindcraft.apoli.common.registry.ApoliPowers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(targets = "com.unascribed.ears.EarsLayerRenderer$1")
public abstract class EarsCompatMixin extends IndirectEarsRenderDelegate<PoseStack, MultiBufferSource, VertexConsumer, AbstractClientPlayer, ModelPart> {

	@WrapOperation(method = "addVertex", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;color(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
	public VertexConsumer inject(VertexConsumer consumer, float r, float g, float b, float alpha, Operation<VertexConsumer> original) {
		ColorConfiguration config = new ColorConfiguration(r, g, b, alpha);
		Optional<ColorConfiguration> opt = ColorConfiguration.forPower(this.peer, ApoliPowers.MODEL_COLOR.get());
		if (opt.isPresent()) {
			config = config.merge(opt.get());
		}
		return original.call(consumer, config.red(), config.green(), config.blue(), config.alpha());
	}
}
