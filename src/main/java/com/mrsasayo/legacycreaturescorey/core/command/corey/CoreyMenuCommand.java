package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

final class CoreyMenuCommand {
    private CoreyMenuCommand() {
    }

    static ArgumentBuilder<ServerCommandSource, ?> createNode() {
        return CommandManager.literal("menu")
            .requires(CoreyCommandPermissions.SUPPORT::test)
            .executes(ctx -> {
                MutableText text = Text.literal("=== Legacy Creatures Menu ===\n");
                text.append(link("📊 Estadísticas", "/corey stats global"));
                text.append("\n");
                text.append(link("🐛 Debug Tools", "/corey debug mob current"));
                text.append("\n");
                text.append(link("🎭 Spawn Manager", "/corey spawn mob minecraft:zombie mythic"));
                text.append("\n");
                text.append(link("🔄 Reload", "/corey reload all"));
                text.append("\n");
                text.append(link("🧬 Forzar Mutación", "/corey mutation force add legacycreaturescorey:swiftness @e[type=minecraft:zombie,limit=1]"));
                ctx.getSource().sendFeedback(() -> text, false);
                return 1;
            });
    }

    private static Text link(String label, String command) {
        return Text.literal(label + " -> " + command);
    }
}
