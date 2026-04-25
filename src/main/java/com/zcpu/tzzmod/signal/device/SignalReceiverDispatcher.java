package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.ModBlock.entity.SignalReceiverBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public final class SignalReceiverDispatcher {
    private SignalReceiverDispatcher() {
    }

    public static int dispatch(SignalEvent event, String channel) {
        if (event == null || event.world() == null) {
            return 0;
        }

        MinecraftServer server = event.world().getServer();
        String normalizedChannel = SignalChannel.normalize(channel);
        int triggered = 0;
        for (SignalDeviceData device : SignalDeviceStore.getEnabledReceiversForChannel(server, normalizedChannel)) {
            ServerWorld receiverWorld = SignalDeviceStore.getDeviceWorld(server, device);
            SignalReceiverBlockEntity receiver = SignalDeviceStore.getLoadedReceiver(server, device);
            if (receiverWorld == null || receiver == null || !receiver.enabled()) {
                continue;
            }
            if (!SignalChannel.normalize(receiver.channel()).equals(normalizedChannel)) {
                continue;
            }

            ActionExecutionResult result = receiver.receiveSignal(receiverWorld);
            if (result.success()) {
                triggered++;
            }
        }
        return triggered;
    }
}
