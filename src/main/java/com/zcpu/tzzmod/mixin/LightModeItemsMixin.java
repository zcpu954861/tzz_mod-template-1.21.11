package com.zcpu.tzzmod.mixin;

import com.zcpu.tzzmod.client.phone.ui.state.PhoneSettingsClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Function;

/**
 * Intercepts item model resolution to serve light-mode texture variants
 * for tzz_mod items when the phone light mode setting is enabled.
 */
@Environment(EnvType.CLIENT)
@Mixin(ItemModelManager.class)
public class LightModeItemsMixin {

    @Shadow private Function<Identifier, ItemModel> modelGetter;

    private static final Set<String> LIGHT_VARIANTS = Set.of(
            "phone",
            "ar_headset",
            "attention",
            "task_configurator",
            "region_planner",
            "map_marker",
            "split_iron_door",
            "blocking_card_configurator",
            "password_config_card"
    );

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void tzz_mod$lightModeUpdate(
            ItemRenderState renderState,
            ItemStack stack,
            ItemDisplayContext displayContext,
            @Nullable World world,
            @Nullable HeldItemContext heldItemContext,
            int seed,
            CallbackInfo ci) {
        if (!PhoneSettingsClient.isLightModeEnabled()) return;

        Identifier id = stack.get(DataComponentTypes.ITEM_MODEL);
        if (id == null
                || !"tzz_mod".equals(id.getNamespace())
                || !LIGHT_VARIANTS.contains(id.getPath())) return;

        Identifier lightId = Identifier.of("tzz_mod", id.getPath() + "_light");
        ItemModel lightModel = this.modelGetter.apply(lightId);
        ClientWorld clientWorld = world instanceof ClientWorld ? (ClientWorld) world : null;

        lightModel.update(
                renderState, stack,
                (ItemModelManager)(Object) this,
                displayContext, clientWorld, heldItemContext, seed
        );
        ci.cancel();
    }
}

