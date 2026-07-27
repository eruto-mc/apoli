package io.github.apace100.apoli.mixin;

import io.github.apace100.apoli.util.MiscUtil;
import io.github.edwinmindcraft.apoli.api.power.INightVisionPower;
import io.github.edwinmindcraft.apoli.common.power.ModifyCameraSubmersionTypePower;
import io.github.edwinmindcraft.apoli.common.power.PhasingPower;
import io.github.edwinmindcraft.apoli.common.power.configuration.PhasingConfiguration;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class BackgroundRendererMixin {

	@WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z", ordinal = 0), method = "setupColor")
	private static boolean hasStatusEffectProxy(LivingEntity player, MobEffect effect, Operation<Boolean> original) {
		if (player instanceof Player && effect == MobEffects.NIGHT_VISION && !player.hasEffect(MobEffects.NIGHT_VISION)) {
			return INightVisionPower.getNightVisionStrength(player).isPresent();
		}
		return original.call(player, effect);
	}

	@ModifyVariable(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;", ordinal = 0), ordinal = 0)
	private static FogType modifyCameraSubmersionTypeRender(FogType original, Camera camera) {
		return ModifyCameraSubmersionTypePower.tryReplace(camera.getEntity(), original).orElse(original);
	}

	@ModifyVariable(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;", ordinal = 0), ordinal = 0)
	private static FogType modifyCameraSubmersionTypeFog(FogType original, Camera camera, FogRenderer.FogMode fogType, float viewDistance, boolean thickFog, float partialTicks) {
		return ModifyCameraSubmersionTypePower.tryReplace(camera.getEntity(), original).orElse(original);
	}

	//I don't know exactly what this does, but it seems harmless.
	@ModifyVariable(method = "setupColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z", ordinal = 0, shift = At.Shift.AFTER), ordinal = 2)
	private static float modifyFogDensityForPhasingBlindness(float original, Camera camera) {
		if (camera.getEntity() instanceof LivingEntity living && PhasingPower.hasRenderMethod(living, PhasingConfiguration.RenderType.BLINDNESS) && MiscUtil.getInWallBlockState(living) != null)
			return 0;
		return original;
	}

    /* Removed in favor of ApoliClientEventHandler#fogColor.
    @ModifyVariable(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getFocusedEntity()Lnet/minecraft/entity/Entity;", ordinal = 1), ordinal = 0)
    private static double modifyD(double original, Camera camera) {
        if(camera.getFocusedEntity() instanceof LivingEntity) {
            if(PowerHolderComponent.getPowers(camera.getFocusedEntity(), PhasingPower.class).stream().anyMatch(pp -> pp.getRenderType() == PhasingPower.RenderType.BLINDNESS)) {
                if(MiscUtil.getInWallBlockState((PlayerEntity)camera.getFocusedEntity()) != null) {
                    RenderSystem.setShaderFogColor(0f, 0f, 0f);
                    return 0;
                }
            }
        }
        return original;
    }*/

    /* Removed in favor of ApoliClientEventHandler#renderForg
    @ModifyVariable(method = "applyFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V"), ordinal = 2)
    private static float modifyFogEndForPhasingBlindness(float original, Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog) {
        if(camera.getFocusedEntity() instanceof LivingEntity) {
            List<PhasingPower> phasings = PowerHolderComponent.getPowers(camera.getFocusedEntity(), PhasingPower.class);
            if(phasings.stream().anyMatch(pp -> pp.getRenderType() == PhasingPower.RenderType.BLINDNESS)) {
                if(MiscUtil.getInWallBlockState((LivingEntity)camera.getFocusedEntity()) != null) {
                    float view = phasings.stream().filter(pp -> pp.getRenderType() == PhasingPower.RenderType.BLINDNESS).map(PhasingPower::getViewDistance).min(Float::compareTo).get();
                    float s;
                    if (fogType == BackgroundRenderer.FogType.FOG_SKY) {
                        s = Math.min(view * 0.8f, original);
                    } else {
                        s = Math.min(view, original);
                    }
                    return s;
                }
            }
        }
        return original;
    }

    @Redirect(method = "applyFog", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V"))
    private static void redirectFogStart(float start, Camera camera, BackgroundRenderer.FogType fogType) {
        if(camera.getFocusedEntity() instanceof LivingEntity) {
            List<PhasingPower> phasings = PowerHolderComponent.getPowers(camera.getFocusedEntity(), PhasingPower.class);
            if(phasings.stream().anyMatch(pp -> pp.getRenderType() == PhasingPower.RenderType.BLINDNESS)) {
                if(MiscUtil.getInWallBlockState((LivingEntity)camera.getFocusedEntity()) != null) {
                    float view = phasings.stream().filter(pp -> pp.getRenderType() == PhasingPower.RenderType.BLINDNESS).map(PhasingPower::getViewDistance).min(Float::compareTo).get();
                    float s;
                    if (fogType == BackgroundRenderer.FogType.FOG_SKY) {
                        s = Math.min(0F, start);
                    } else {
                        if(camera.getSubmersionType() == CameraSubmersionType.WATER) {
                            s = Math.min(-4.0f, start);
                        } else {
                            s = Math.min(view * 0.25F, start);
                        }
                    }
                    RenderSystem.setShaderFogStart(s);
                    return;
                }
            }
        }
        RenderSystem.setShaderFogStart(start);
    }*/
}
