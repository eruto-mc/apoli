package io.github.apace100.apoli.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.common.power.InvisibilityPower;
import io.github.edwinmindcraft.apoli.common.power.PreventFeatureRenderPower;
import io.github.edwinmindcraft.apoli.common.power.configuration.ColorConfiguration;
import io.github.edwinmindcraft.apoli.common.registry.ApoliPowers;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin extends EntityRenderer<LivingEntity> {

	protected LivingEntityRendererMixin(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Inject(method = "isShaking", at = @At("HEAD"), cancellable = true)
	private void letPlayersShakeTheirBodies(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (IPowerContainer.hasPower(entity, ApoliPowers.SHAKING.get()))
			cir.setReturnValue(true);
	}

    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;"), ordinal = 2)
    private boolean preventOutlineRendering(boolean original, LivingEntity livingEntity) {
        if (InvisibilityPower.isOutlineHidden(livingEntity))
            return false;
        return original;
    }

	@ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;", shift = At.Shift.BEFORE))
	private RenderType changeRenderLayerWhenTranslucent(RenderType original, LivingEntity entity) {
		if (entity instanceof Player) {
			return ColorConfiguration.forPower(entity, ApoliPowers.MODEL_COLOR.get()).filter(x -> x.alpha() < 1F)
					.map(x -> RenderType.itemEntityTranslucentCull(this.getTextureLocation(entity))).orElse(original);
		}
		return original;
	}

	@WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"))
	private void renderColorChangedModel(EntityModel<LivingEntity> model, PoseStack postStack, VertexConsumer vertexConsumer, int p1, int overlay, float red, float green, float blue, float alpha, Operation<Void> original, LivingEntity living) {
		Optional<ColorConfiguration> opt = ColorConfiguration.forPower(living, ApoliPowers.MODEL_COLOR.get());
		if (opt.isPresent()) {
			ColorConfiguration color = opt.get();
			red *= color.red();
			green *= color.green();
			blue *= color.blue();
			alpha *= color.alpha();
		}
		original.call(model, postStack, vertexConsumer, p1, overlay, red, green, blue, alpha);
	}

	@WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"))
	private <T extends Entity> void preventFeatureRendering(RenderLayer<T, ?> instance, PoseStack poseStack, MultiBufferSource buffer, int packedLight, T living, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, Operation<Void> original) {
		if (PreventFeatureRenderPower.doesPrevent(living, instance) || InvisibilityPower.isArmorHidden(living))
			return;
		original.call(instance, poseStack, buffer, packedLight, living, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
	}
}
