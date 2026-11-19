package net.am7n.shardclient.full.modules.utility;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.platform.Client;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.am7n.shardclient.protocol.packet.MatchEnded;
import net.am7n.shardclient.shared.ShardClientRuntime;
import net.am7n.shardclient.shared.practice.PracticeListener;

public final class AutoGG extends Module {
    private final ChoiceSetting message = choice("message", "Message", "gg", "gg", "gg wp", "GG", "good game");
    private int sendIn = -1;

    public AutoGG() {
        super("auto_gg", "Auto GG", "Sends gg in chat when a ShardPvP match ends.", Category.UTILITY);
        ShardClientRuntime.get().practice().session().addListener(new PracticeListener() {
            @Override
            public void onMatchEnded(MatchEnded result) {
                if (isEnabled()) {
                    sendIn = 15;
                }
            }
        });
    }

    @Override
    public void tick() {
        if (sendIn > 0 && --sendIn == 0) {
            sendIn = -1;
            if (mc.player != null && mc.getConnection() != null) {
                Client.sendChat(message.get());
            }
        }
    }
}
