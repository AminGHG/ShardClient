package net.am7n.shardclient.full.modules.utility;

import net.am7n.shardclient.full.module.Category;
import net.am7n.shardclient.full.module.Module;
import net.am7n.shardclient.full.module.setting.ChoiceSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ChatTimestamps extends Module {
    private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final ChoiceSetting format = choice("format", "Format", "24 h", "24 h", "12 h");

    public ChatTimestamps() {
        super("chat_timestamps", "Chat timestamps", "Shows when each chat message arrived.", Category.UTILITY);
    }

    public Component stamp(Component message) {
        String time = LocalTime.now().format(format.is("12 h") ? H12 : H24);
        return Component.empty().append(Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY)).append(message);
    }
}
