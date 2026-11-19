package net.am7n.shardclient.full.platform;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.network.chat.Component;

import java.util.function.IntFunction;

public final class Commands {
    private Commands() {
    }

    public static void number(String name, String argument, int min, int max, IntFunction<Component> run) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommandManager.literal(name)
                        .then(ClientCommandManager.argument(argument, IntegerArgumentType.integer(min, max))
                                .executes(command -> {
                                    int value = IntegerArgumentType.getInteger(command, argument);
                                    command.getSource().sendFeedback(run.apply(value));
                                    return value;
                                }))));
    }
}
