package com.zcpu.tzzmod.core.bootstrap;

import com.zcpu.tzzmod.network.AdminPayloads;
import com.zcpu.tzzmod.network.BlockingCardPayloads;
import com.zcpu.tzzmod.network.DeathStatusPayload;
import com.zcpu.tzzmod.network.GalleryPayloads;
import com.zcpu.tzzmod.network.MapPayloads;
import com.zcpu.tzzmod.network.NotePayloads;
import com.zcpu.tzzmod.network.PasswordPayloads;
import com.zcpu.tzzmod.network.PhoneChatPayloads;
import com.zcpu.tzzmod.network.TaskPayloads;

public final class TzzNetworkBootstrap {
    private TzzNetworkBootstrap() {
    }

    public static void register() {
        DeathStatusPayload.register();
        PhoneChatPayloads.register();
        MapPayloads.register();
        TaskPayloads.register();
        PasswordPayloads.register();
        BlockingCardPayloads.register();
        GalleryPayloads.register();
        NotePayloads.register();
        AdminPayloads.register();
    }
}
