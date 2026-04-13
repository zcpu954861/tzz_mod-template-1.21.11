package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.blocking.BlockingCardConfig;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class BlockingCardItem extends Item {
    public BlockingCardItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (BlockingCardConfig.read(user.getStackInHand(hand)).isConfigured()) {
            return ActionResult.PASS;
        }
        user.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if (BlockingCardConfig.read(stack).isConfigured()) {
            textConsumer.accept(Text.translatable("item.tzz_mod.blocking_card.configured_lore").formatted(Formatting.GOLD));
        }
    }
}
