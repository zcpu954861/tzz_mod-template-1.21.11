package com.zcpu.tzzmod.ModItem.custom;

import com.zcpu.tzzmod.blocking.BlockingCardConfig;
import com.zcpu.tzzmod.blocking.BlockingCardConfiguratorClientAccess;
import com.zcpu.tzzmod.blocking.BlockingCardConfiguratorState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class BlockingCardConfiguratorItem extends Item {
    public BlockingCardConfiguratorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }
            return handleStorageInteraction(user, hand);
        }

        if (world.isClient()) {
            BlockingCardConfiguratorClientAccess.openScreen(hand);
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }

        if (player.isSneaking()) {
            if (context.getWorld().isClient()) {
                return ActionResult.SUCCESS;
            }
            return handleStorageInteraction(player, context.getHand());
        }

        if (context.getWorld().isClient()) {
            BlockingCardConfiguratorClientAccess.openScreen(context.getHand());
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
    }

    private ActionResult handleStorageInteraction(PlayerEntity user, Hand hand) {
        ItemStack configurator = user.getStackInHand(hand);
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack otherStack = user.getStackInHand(otherHand);

        if (BlockingCardConfiguratorState.hasStoredCards(configurator)) {
            if (!otherStack.isEmpty()) {
                user.sendMessage(Text.translatable("item.tzz_mod.blocking_card_configurator.need_empty_other_hand"), true);
                return ActionResult.CONSUME;
            }

            ItemStack extracted = BlockingCardConfiguratorState.extract(configurator);
            if (extracted.isEmpty()) {
                user.sendMessage(Text.translatable("item.tzz_mod.blocking_card_configurator.no_cards_loaded"), true);
                return ActionResult.CONSUME;
            }

            user.setStackInHand(otherHand, extracted);
            user.getEntityWorld().playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6F, 1.1F);
            user.sendMessage(Text.translatable("item.tzz_mod.blocking_card_configurator.unloaded", extracted.getName(), extracted.getCount()), true);
            return ActionResult.CONSUME;
        }

        if (otherStack.isEmpty()) {
            user.sendMessage(Text.translatable("item.tzz_mod.blocking_card_configurator.insert_prompt"), true);
            return ActionResult.CONSUME;
        }
        if (!BlockingCardConfig.isBlockingCard(otherStack)) {
            user.sendMessage(Text.translatable("item.tzz_mod.blocking_card_configurator.only_blocking_cards"), true);
            return ActionResult.CONSUME;
        }

        BlockingCardConfiguratorState.store(configurator, otherStack);
        user.setStackInHand(otherHand, ItemStack.EMPTY);
        user.getEntityWorld().playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.65F, 1.3F);
        user.sendMessage(Text.translatable("item.tzz_mod.blocking_card_configurator.loaded", otherStack.getName(), otherStack.getCount()), true);
        return ActionResult.CONSUME;
    }
}