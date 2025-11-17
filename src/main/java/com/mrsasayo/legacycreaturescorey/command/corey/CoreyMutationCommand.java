package com.mrsasayo.legacycreaturescorey.command.corey;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrsasayo.legacycreaturescorey.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.Mutation;
import com.mrsasayo.legacycreaturescorey.mutation.MutationRegistry;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

final class CoreyMutationCommand {
    private CoreyMutationCommand() {
    }

    static ArgumentBuilder<ServerCommandSource, ?> createNode() {
        LiteralArgumentBuilder<ServerCommandSource> mutations = CommandManager.literal("mutation")
            .requires(CoreyCommandPermissions.GAMEPLAY::test);

        mutations.then(CommandManager.literal("list")
            .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                .executes(ctx -> listMutations(ctx, EntityArgumentType.getEntity(ctx, "objetivo")))));

        mutations.then(CommandManager.literal("clear")
            .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                .executes(ctx -> clearMutations(ctx, EntityArgumentType.getEntity(ctx, "objetivo")))));

        mutations.then(CommandManager.literal("force")
            .then(CommandManager.literal("add")
                .then(CommandManager.argument("mutacion", IdentifierArgumentType.identifier())
                    .suggests(CoreyCommandShared.MUTATION_SUGGESTIONS)
                    .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                        .executes(ctx -> forceMutations(ctx,
                            List.of(IdentifierArgumentType.getIdentifier(ctx, "mutacion")),
                            false)))))
            .then(CommandManager.literal("replace")
                .then(CommandManager.argument("mutaciones", StringArgumentType.greedyString())
                    .suggests(CoreyCommandShared.MUTATION_LIST_SUGGESTIONS)
                    .then(CommandManager.argument("objetivo", EntityArgumentType.entity())
                        .executes(ctx -> forceMutations(ctx,
                            CoreyCommandShared.parseMutations(StringArgumentType.getString(ctx, "mutaciones")),
                            true))))));

        return mutations;
    }

    private static int listMutations(CommandContext<ServerCommandSource> ctx, Entity entity) {
        if (!(entity instanceof MobEntity mob)) {
            ctx.getSource().sendError(Text.literal("El objetivo no es un mob válido."));
            return 0;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        StringBuilder builder = new StringBuilder();
        builder.append("Mutaciones de ").append(mob.getDisplayName().getString()).append(" (" + data.getTier().getDisplayName() + ")\n");
        if (data.getMutations().isEmpty()) {
            builder.append("(sin mutaciones)\n");
        } else {
            int index = 1;
            for (Identifier id : data.getMutations()) {
                Mutation mutation = MutationRegistry.get(id);
                builder.append(index++).append(". ").append(id);
                if (mutation != null && mutation.getDisplayName() != null) {
                    builder.append(" (").append(mutation.getDisplayName().getString()).append(")");
                }
                builder.append('\n');
            }
        }
        ctx.getSource().sendFeedback(() -> Text.literal(builder.toString()), false);
        return data.getMutations().size();
    }

    private static int clearMutations(CommandContext<ServerCommandSource> ctx, Entity entity) {
        if (!(entity instanceof MobEntity mob)) {
            ctx.getSource().sendError(Text.literal("El objetivo no es un mob válido."));
            return 0;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        if (data.getMutations().isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("El mob no tiene mutaciones activas."), false);
            return 0;
        }
        removeAllMutations(mob, data);
        ctx.getSource().sendFeedback(() -> Text.literal("Mutaciones eliminadas de " + mob.getDisplayName().getString()), true);
        return 1;
    }

    private static int forceMutations(CommandContext<ServerCommandSource> ctx, List<Identifier> requested, boolean replaceExisting) throws CommandSyntaxException {
        if (requested == null || requested.isEmpty()) {
            ctx.getSource().sendError(Text.literal("Debes especificar al menos una mutación."));
            return 0;
        }
        Entity entity = EntityArgumentType.getEntity(ctx, "objetivo");
        if (!(entity instanceof MobEntity mob)) {
            ctx.getSource().sendError(Text.literal("El objetivo no es un mob válido."));
            return 0;
        }
        return applyMutations(ctx.getSource(), mob, requested, replaceExisting);
    }

    private static int applyMutations(ServerCommandSource source, MobEntity mob, List<Identifier> requested, boolean replaceExisting) {
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        List<Identifier> original = new ArrayList<>(data.getMutations());
        if (replaceExisting && !original.isEmpty()) {
            removeAllMutations(mob, data);
        }
        List<Identifier> working = new ArrayList<>(data.getMutations());
        int applied = 0;
        for (Identifier id : requested) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation == null) {
                source.sendError(Text.literal("Mutación desconocida: " + id));
                continue;
            }
            String failure = mutation.getApplyFailureReason(mob, working);
            if (failure != null) {
                source.sendError(Text.literal("No se pudo aplicar " + id + ": " + failure));
                continue;
            }
            mutation.onApply(mob);
            data.addMutation(id);
            working.add(id);
            applied++;
        }

        if (applied == 0 && replaceExisting && !original.isEmpty()) {
            restoreMutations(mob, data, original);
        }

        int totalApplied = applied;
        if (totalApplied > 0) {
            source.sendFeedback(() -> Text.literal("Mutaciones aplicadas a " + mob.getDisplayName().getString() + ": " + totalApplied), true);
        }
        return totalApplied;
    }

    private static void removeAllMutations(MobEntity mob, MobLegacyData data) {
        List<Identifier> existing = new ArrayList<>(data.getMutations());
        if (existing.isEmpty()) {
            return;
        }
        data.clearMutations();
        for (Identifier id : existing) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation != null) {
                mutation.onRemove(mob);
            }
        }
    }

    private static void restoreMutations(MobEntity mob, MobLegacyData data, List<Identifier> previous) {
        data.clearMutations();
        for (Identifier id : previous) {
            Mutation mutation = MutationRegistry.get(id);
            if (mutation != null) {
                mutation.onApply(mob);
                data.addMutation(id);
            }
        }
    }
}
