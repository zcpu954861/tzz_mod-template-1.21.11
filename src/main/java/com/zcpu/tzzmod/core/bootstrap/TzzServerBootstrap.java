package com.zcpu.tzzmod.core.bootstrap;

import com.zcpu.tzzmod.blocking.BlockingCardServer;
import com.zcpu.tzzmod.blocking.BlockingCardUseHandler;
import com.zcpu.tzzmod.gallery.GalleryServer;
import com.zcpu.tzzmod.map.MapServer;
import com.zcpu.tzzmod.network.AdminSyncServer;
import com.zcpu.tzzmod.network.DeathSyncServer;
import com.zcpu.tzzmod.note.NoteServer;
import com.zcpu.tzzmod.password.PasswordServer;
import com.zcpu.tzzmod.phone.chat.PhoneChatServer;
import com.zcpu.tzzmod.region.RegionControllerServer;
import com.zcpu.tzzmod.task.TaskServer;

public final class TzzServerBootstrap {
    private TzzServerBootstrap() {
    }

    public static void register() {
        DeathSyncServer.register();
        PhoneChatServer.register();
        MapServer.register();
        TaskServer.register();
        PasswordServer.register();
        BlockingCardServer.register();
        BlockingCardUseHandler.register();
        GalleryServer.register();
        NoteServer.register();
        AdminSyncServer.register();
        RegionControllerServer.register();
    }
}
