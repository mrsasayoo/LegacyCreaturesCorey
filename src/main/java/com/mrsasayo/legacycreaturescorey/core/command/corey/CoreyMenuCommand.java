package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

final class CoreyMenuCommand {
    private CoreyMenuCommand() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> createNode() {
        return Commands.literal("menu")
            .requires(CoreyCommandPermissions.SUPPORT::test)
            .executes(ctx -> {
                MutableComponent text = Component.literal("=== Legacy Creatures Menu ===\n");
                text.append(link("📊 Estadísticas", "/corey stats global"));
                text.append("\n");
                text.append(link("🐛 Debug Tools", "/corey debug mob current"));
                text.append("\n");
                text.append(link("🎭 Spawn Manager", "/corey spawn mob minecraft:zombie mythic"));
                text.append("\n");
                text.append(link("🔄 Reload", "/corey reload all"));
                text.append("\n");
                text.append(link("🧬 Forzar Mutación", "/corey mutation force add legacycreaturescorey:swiftness @e[type=minecraft:zombie,limit=1]"));
                ctx.getSource().sendSuccess(() -> text, false);
                return 1;
            });
    }

    private static Component link(String label, String command) {
        return Component.literal(label + " -> " + command);
    }
}
