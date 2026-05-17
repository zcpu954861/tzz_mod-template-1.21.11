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
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceContainerHandler;
import com.zcpu.tzzmod.signal.device.VirtualBlockDeviceInteractionHandler;
import com.zcpu.tzzmod.scheduler.TimerServer;
import com.zcpu.tzzmod.webadmin.container.WebAdminContainerTemplateServer;
import com.zcpu.tzzmod.webadmin.itemsubmit.WebAdminSingleItemSubmitTemplateServer;
import com.zcpu.tzzmod.task.TaskServer;
import com.zcpu.tzzmod.webadmin.selection.WebAdminSelectionServer;
import com.zcpu.tzzmod.webadmin.testbridge.WebAdminTestBridgeGuiServer;

public final class TzzServerBootstrap {
    private TzzServerBootstrap() {
    }

    public static void register() {
        TimerServer.register();
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
        VirtualBlockDeviceInteractionHandler.register();
        VirtualBlockDeviceContainerHandler.register();
        WebAdminSelectionServer.register();
        WebAdminContainerTemplateServer.register();
        WebAdminSingleItemSubmitTemplateServer.register();
        WebAdminTestBridgeGuiServer.register();
    }
}
