package com.zcpu.tzzmod.signal.device;

import com.zcpu.tzzmod.ModBlock.entity.ActionRelayBlockEntity;
import com.zcpu.tzzmod.action.ActionExecutionResult;
import com.zcpu.tzzmod.signal.SignalChannel;
import com.zcpu.tzzmod.signal.SignalEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public final class ActionRelayDispatcher {
    private ActionRelayDispatcher() {
    }

    public static DispatchResult dispatch(SignalEvent event, String channel) {
        if (event == null || event.world() == null) {
            return DispatchResult.empty();
        }

        MinecraftServer server = event.world().getServer();
        String normalizedChannel = SignalChannel.normalize(channel);
        int executed = 0;
        int failed = 0;
        for (SignalDeviceData device : SignalDeviceStore.getEnabledActionRelaysForChannel(server, normalizedChannel)) {
            ServerWorld relayWorld = SignalDeviceStore.getDeviceWorld(server, device);
            ActionRelayBlockEntity relay = SignalDeviceStore.getLoadedActionRelay(server, device);
            if (relayWorld == null || relay == null || !relay.enabled()) {
                continue;
            }
            if (!SignalChannel.normalize(relay.channel()).equals(normalizedChannel)) {
                continue;
            }

            ActionExecutionResult result = relay.executeRelayActions(relayWorld, event, false);
            if (result.success()) {
                executed++;
            } else {
                failed++;
            }
        }
        return new DispatchResult(executed, failed);
    }

    public record DispatchResult(int executedCount, int failedCount) {
        public static DispatchResult empty() {
            return new DispatchResult(0, 0);
        }
    }
}
