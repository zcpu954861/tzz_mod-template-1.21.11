package com.zcpu.tzzmod.core.bootstrap;

import com.zcpu.tzzmod.ModBlock.ModBlockEntities;
import com.zcpu.tzzmod.ModBlock.ModBlocks;
import com.zcpu.tzzmod.ModItem.ModFunctionItemGroup;
import com.zcpu.tzzmod.ModItem.ModItemGroup;
import com.zcpu.tzzmod.ModItem.ModItems;

public final class TzzContentBootstrap {
    private TzzContentBootstrap() {
    }

    public static void register() {
        ModItems.initialize();
        ModItemGroup.inialize();
        ModFunctionItemGroup.inialize();
        ModBlocks.init();
        ModBlockEntities.init();
    }
}
