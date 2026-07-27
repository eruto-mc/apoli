package io.github.apace100.apoli.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.edwinmindcraft.apoli.common.power.configuration.ColorConfiguration;
import io.github.edwinmindcraft.apoli.common.registry.ApoliPowers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(PlayerRenderer.class)
public class PlayerEntityRendererMixin {

	private static void colorizeHand(ModelPart modelPart, PoseStack matrices, VertexConsumer vertices, int light, int overlay, MultiBufferSource vertexConsumers, AbstractClientPlayer player, Operation<Void> original) {
		Optional<ColorConfiguration> colorConfiguration = ColorConfiguration.forPower(player, ApoliPowers.MODEL_COLOR.get());
		if (colorConfiguration.isPresent()) {
			ColorConfiguration color = colorConfiguration.get();
			modelPart.render(matrices, vertexConsumers.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation())), light, overlay, color.red(), color.green(), color.blue(), color.alpha());
		} else
			original.call(modelPart, matrices, vertices, light, overlay);
	}

	@OnlyIn(Dist.CLIENT)
	@WrapOperation(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", ordinal = 0))
	private void makeArmTranslucent(ModelPart modelPart, PoseStack matrices, VertexConsumer vertices, int light, int overlay, Operation<Void> original, PoseStack matrices2, MultiBufferSource vertexConsumers, int light2, AbstractClientPlayer player) {
		colorizeHand(modelPart, matrices, vertices, light, overlay, vertexConsumers, player, original);
	}

	@OnlyIn(Dist.CLIENT)
	@WrapOperation(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V", ordinal = 1))
	private void makeSleeveTranslucent(ModelPart modelPart, PoseStack matrices, VertexConsumer vertices, int light, int overlay, Operation<Void> original, PoseStack matrices2, MultiBufferSource vertexConsumers, int light2, AbstractClientPlayer player) {
		colorizeHand(modelPart, matrices, vertices, light, overlay, vertexConsumers, player, original);
	}
}
