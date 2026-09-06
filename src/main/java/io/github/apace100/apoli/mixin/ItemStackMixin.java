package io.github.apace100.apoli.mixin;

import io.github.apace100.apoli.access.EntityLinkedItemStack;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.VariableAccess;
import io.github.edwinmindcraft.apoli.common.power.ActionOnItemUsePower;
import io.github.edwinmindcraft.apoli.common.power.ItemOnItemPower;
import io.github.edwinmindcraft.apoli.common.power.configuration.ActionOnItemUseConfiguration;
import io.github.edwinmindcraft.apoli.common.registry.ApoliCapabilities;
import io.github.edwinmindcraft.apoli.common.registry.ApoliPowers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin extends net.minecraftforge.common.capabilities.CapabilityProvider<ItemStack>  {

    @Shadow public abstract int getUseDuration();

    @Shadow public abstract InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand);

    protected ItemStackMixin(Class<ItemStack> baseClass) {
        super(baseClass);
    }

    //Moved from ItemMixin to prevent overrides from other mods from interfering too much.
	@Inject(method = "overrideOtherStackedOnMe", at = @At("RETURN"), cancellable = true)
	public void forgeItem(ItemStack other, Slot slot, ClickAction pAction, Player pPlayer, SlotAccess otherAccess, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue())
			return;
		if (pAction != ClickAction.SECONDARY)
			return;
		if (ItemOnItemPower.execute(pPlayer, slot, otherAccess))
			cir.setReturnValue(true);
	}

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private Item callActionOnUseInstantBefore(ItemStack original, Level world, Player user, InteractionHand hand) {
        if (ApoliAPI.getPowerContainer(user) != null && ApoliAPI.getPowerContainer(user).getPowers(ApoliPowers.ACTION_ON_ITEM_USE.get()).stream().anyMatch(p -> p.isBound() && p.value().getFactory().canRun(p, user, (ItemStack)(Object)this, this.getUseDuration() == 0 ? ActionOnItemUseConfiguration.TriggerType.INSTANT : ActionOnItemUseConfiguration.TriggerType.START, ActionOnItemUseConfiguration.PriorityPhase.BEFORE))) {
            MutableObject<ItemStack> mutable = new MutableObject<>(original);
            ActionOnItemUsePower.execute(user, original, mutable, this.getUseDuration() == 0 ? ActionOnItemUseConfiguration.TriggerType.INSTANT : ActionOnItemUseConfiguration.TriggerType.START, ActionOnItemUseConfiguration.PriorityPhase.BEFORE);
            return mutable.getValue().getItem();
        }
        return original.getItem();
    }

    // This is probably the wrong way to do this but I really didn't want to set up NBT for the capability. Oh well.
    //
    // ⚠ eruto: ask the SOURCE stack first, not the copy (2026-09-06).
    //
    // Forge gathers an ItemStack's capabilities lazily: CapabilityProvider.getCapabilities() runs
    // doGatherCapabilities() -- which posts AttachCapabilitiesEvent to the whole bus -- the first
    // time getCapability() is called, then sets `initialized` so it never runs again for that
    // object. (Read out of forge-1.20.1-47.4.22-universal.jar with javap.)
    //
    // copy() returns a brand new ItemStack, so asking the COPY first guaranteed one full
    // AttachCapabilitiesEvent post per copy, forever. Asking the SOURCE first costs that once per
    // stack object and nothing afterwards, because a stack sitting in an inventory is the same
    // object every tick. When the source has no linked entity we return without ever touching the
    // copy, so no gather happens on it at all.
    //
    // Measured on the club's rental server (spark, 2026-09-06, two players, 1 tick = 81.77 ms):
    // Tom's Simple Storage rebuilds its terminal list every tick while a player has the screen
    // open, one ItemStack.copy() per slot. That path cost 13.13 ms/tick, of which 9.88 ms/tick was
    // this handler's gather -> ForgeEventFactory.gatherCapabilities -> EventBus.post.
    //
    // Behaviour is unchanged: a stack with no ENTITY_LINKED_ITEM_STACK value, or one whose value
    // has a null entity, had nothing to copy across in the first place.
    @Inject(method = "copy", at = @At(value = "RETURN"))
    private void copyNewParams(CallbackInfoReturnable<ItemStack> cir) {
        Optional<EntityLinkedItemStack> otherEli = this.getCapability(ApoliCapabilities.ENTITY_LINKED_ITEM_STACK).resolve();
        if (otherEli.isEmpty() || otherEli.get().getEntity() == null) {
            return;
        }
        cir.getReturnValue().getCapability(ApoliCapabilities.ENTITY_LINKED_ITEM_STACK).ifPresent(eli -> {
            eli.setEntity(otherEli.get().getEntity());
        });
    }

    // TODO: When Origins Fabric gets MixinExtras, use @ModifyReturnValue.
    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    private void callActionOnUseInstantAfter(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (cir.getReturnValue().getResult().consumesAction() && ApoliAPI.getPowerContainer(user) != null && ApoliAPI.getPowerContainer(user).getPowers(ApoliPowers.ACTION_ON_ITEM_USE.get()).stream().anyMatch(p -> p.isBound() && p.value().getFactory().canRun(p, user, (ItemStack)(Object)this, this.getUseDuration() == 0 ? ActionOnItemUseConfiguration.TriggerType.INSTANT : ActionOnItemUseConfiguration.TriggerType.START, ActionOnItemUseConfiguration.PriorityPhase.AFTER))) {
            MutableObject<ItemStack> mutable = new MutableObject<>(cir.getReturnValue().getObject());
            ActionOnItemUsePower.execute(user, cir.getReturnValue().getObject(), mutable, this.getUseDuration() == 0 ? ActionOnItemUseConfiguration.TriggerType.INSTANT : ActionOnItemUseConfiguration.TriggerType.START, ActionOnItemUseConfiguration.PriorityPhase.AFTER);
            cir.setReturnValue(new InteractionResultHolder<>(cir.getReturnValue().getResult(), mutable.getValue()));
        }
    }
}
