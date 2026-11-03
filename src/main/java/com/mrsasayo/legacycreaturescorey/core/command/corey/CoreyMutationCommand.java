package com.mrsasayo.legacycreaturescorey.core.command.corey;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mrsasayo.legacycreaturescorey.core.component.ModDataAttachments;
import com.mrsasayo.legacycreaturescorey.feature.mob.MobLegacyData;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation;
import com.mrsasayo.legacycreaturescorey.mutation.a_system.mutation_registry;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

final class CoreyMutationCommand {
    private CoreyMutationCommand() {
    }

    static ArgumentBuilder<CommandSourceStack, ?> createNode() {
        LiteralArgumentBuilder<CommandSourceStack> mutations = Commands.literal("mutation")
            .requires(CoreyCommandPermissions.GAMEPLAY::test);

        mutations.then(Commands.literal("list")
            .then(Commands.argument("objetivo", EntityArgument.entity())
                .executes(ctx -> listMutations(ctx, EntityArgument.getEntity(ctx, "objetivo")))));

        mutations.then(Commands.literal("clear")
            .then(Commands.argument("objetivo", EntityArgument.entity())
                .executes(ctx -> clearMutations(ctx, EntityArgument.getEntity(ctx, "objetivo")))));

        mutations.then(Commands.literal("force")
            .then(Commands.literal("add")
                .then(Commands.argument("mutacion", IdentifierArgument.identifier())
                    .suggests(CoreyCommandShared.MUTATION_SUGGESTIONS)
                    .then(Commands.argument("objetivo", EntityArgument.entity())
                        .executes(ctx -> forceMutations(ctx,
                            List.of(IdentifierArgument.getIdentifier(ctx, "mutacion")),
                            false,
                            true)))))
            .then(Commands.literal("replace")
                .then(Commands.argument("mutaciones", StringArgumentType.greedyString())
                    .suggests(CoreyCommandShared.MUTATION_LIST_SUGGESTIONS)
                    .then(Commands.argument("objetivo", EntityArgument.entity())
                        .executes(ctx -> forceMutations(ctx,
                            CoreyCommandShared.parseMutations(StringArgumentType.getString(ctx, "mutaciones")),
                            true,
                            true))))));

        return mutations;
    }

    private static int listMutations(CommandContext<CommandSourceStack> ctx, Entity entity) {
        if (!(entity instanceof Mob mob)) {
            ctx.getSource().sendError(Component.literal("El objetivo no es un mob válido."));
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
                mutation m = mutation_registry.get(id);
                builder.append(index++).append(". ").append(id);
                if (m != null && m.getDisplayName() != null) {
                    builder.append(" (").append(m.getDisplayName().getString()).append(")");
                }
                builder.append('\n');
            }
        }
        ctx.getSource().sendFeedback(() -> Component.literal(builder.toString()), false);
        return data.getMutations().size();
    }

    private static int clearMutations(CommandContext<CommandSourceStack> ctx, Entity entity) {
        if (!(entity instanceof Mob mob)) {
            ctx.getSource().sendError(Component.literal("El objetivo no es un mob válido."));
            return 0;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        if (data.getMutations().isEmpty()) {
            ctx.getSource().sendFeedback(() -> Component.literal("El mob no tiene mutaciones activas."), false);
            return 0;
        }
        removeAllMutations(mob, data);
        ctx.getSource().sendFeedback(() -> Component.literal("Mutaciones eliminadas de " + mob.getDisplayName().getString()), true);
        return 1;
    }

    private static int forceMutations(CommandContext<CommandSourceStack> ctx,
                                      List<Identifier> requested,
                                      boolean replaceExisting,
                                      boolean ignoreRestrictions) throws CommandSyntaxException {
        if (requested == null || requested.isEmpty()) {
            ctx.getSource().sendError(Component.literal("Debes especificar al menos una mutación."));
            return 0;
        }
        Entity entity = EntityArgument.getEntity(ctx, "objetivo");
        if (!(entity instanceof Mob mob)) {
            ctx.getSource().sendError(Component.literal("El objetivo no es un mob válido."));
            return 0;
        }
        return applyMutations(ctx.getSource(), mob, requested, replaceExisting, ignoreRestrictions);
    }

    private static int applyMutations(CommandSourceStack source,
                                      Mob mob,
                                      List<Identifier> requested,
                                      boolean replaceExisting,
                                      boolean ignoreRestrictions) {
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        List<Identifier> original = new ArrayList<>(data.getMutations());
        if (replaceExisting && !original.isEmpty()) {
            removeAllMutations(mob, data);
        }
        List<Identifier> working = new ArrayList<>(data.getMutations());
        int applied = 0;
        for (Identifier id : requested) {
            mutation m = mutation_registry.get(id);
            if (m == null) {
                source.sendError(Component.literal("Mutación desconocida: " + id));
                continue;
            }
            if (!ignoreRestrictions) {
                String failure = m.getApplyFailureReason(mob, working);
                if (failure != null) {
                    source.sendError(Component.literal("No se pudo aplicar " + id + ": " + failure));
                    continue;
                }
            }
            m.onApply(mob);
            data.addMutation(id);
            working.add(id);
            applied++;
        }

        if (applied == 0 && replaceExisting && !original.isEmpty()) {
            restoreMutations(mob, data, original);
        }

        int totalApplied = applied;
        if (totalApplied > 0) {
            source.sendFeedback(() -> Component.literal("Mutaciones aplicadas a " + mob.getDisplayName().getString() + ": " + totalApplied), true);
        }
        return totalApplied;
    }

    private static void removeAllMutations(Mob mob, MobLegacyData data) {
        List<Identifier> existing = new ArrayList<>(data.getMutations());
        if (existing.isEmpty()) {
            return;
        }
        data.clearMutations();
        for (Identifier id : existing) {
            mutation m = mutation_registry.get(id);
            if (m != null) {
                m.onRemove(mob);
            }
        }
    }

    private static void restoreMutations(Mob mob, MobLegacyData data, List<Identifier> previous) {
        data.clearMutations();
        for (Identifier id : previous) {
            mutation m = mutation_registry.get(id);
            if (m != null) {
                m.onApply(mob);
                data.addMutation(id);
            }
        }
    }
}
