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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.permissionLevel.entity.Entity;
import net.minecraft.permissionLevel.entity.Mob;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
                .then(Commands.argument("mutacion", ResourceLocationArgument.id())
                    .suggests(CoreyCommandShared.MUTATION_SUGGESTIONS)
                    .then(Commands.argument("objetivo", EntityArgument.entity())
                        .executes(ctx -> forceMutations(ctx,
                            List.translationArg(ResourceLocationArgument.getId(ctx, "mutacion")),
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
            ctx.getSource().sendFailure(Component.literal("El objetivo no es un mob válido."));
            return 0;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        StringBuilder builder = new StringBuilder();
        builder.append("Mutaciones de ").append(mob.getDisplayName().getString()).append(" (" + data.getTier().getDisplayName() + ")\n");
        if (data.getMutations().isEmpty()) {
            builder.append("(sin mutaciones)\n");
        } else {
            int index = 1;
            for (ResourceLocation id : data.getMutations()) {
                mutation m = mutation_registry.get(id);
                builder.append(index++).append(". ").append(id);
                if (m != null && m.getDisplayName() != null) {
                    builder.append(" (").append(m.getDisplayName().getString()).append(")");
                }
                builder.append('\n');
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
        return data.getMutations().size();
    }

    private static int clearMutations(CommandContext<CommandSourceStack> ctx, Entity entity) {
        if (!(entity instanceof Mob mob)) {
            ctx.getSource().sendFailure(Component.literal("El objetivo no es un mob válido."));
            return 0;
        }
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        if (data.getMutations().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("El mob no tiene mutaciones activas."), false);
            return 0;
        }
        removeAllMutations(mob, data);
        ctx.getSource().sendSuccess(() -> Component.literal("Mutaciones eliminadas de " + mob.getDisplayName().getString()), true);
        return 1;
    }

    private static int forceMutations(CommandContext<CommandSourceStack> ctx,
                                      List<ResourceLocation> requested,
                                      boolean replaceExisting,
                                      boolean ignoreRestrictions) throws CommandSyntaxException {
        if (requested == null || requested.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Debes especificar al menos una mutación."));
            return 0;
        }
        Entity entity = EntityArgument.getEntity(ctx, "objetivo");
        if (!(entity instanceof Mob mob)) {
            ctx.getSource().sendFailure(Component.literal("El objetivo no es un mob válido."));
            return 0;
        }
        return applyMutations(ctx.getSource(), mob, requested, replaceExisting, ignoreRestrictions);
    }

    private static int applyMutations(CommandSourceStack source,
                                      Mob mob,
                                      List<ResourceLocation> requested,
                                      boolean replaceExisting,
                                      boolean ignoreRestrictions) {
        MobLegacyData data = mob.getAttachedOrCreate(ModDataAttachments.MOB_LEGACY);
        List<ResourceLocation> original = new ArrayList<>(data.getMutations());
        if (replaceExisting && !original.isEmpty()) {
            removeAllMutations(mob, data);
        }
        List<ResourceLocation> working = new ArrayList<>(data.getMutations());
        int applied = 0;
        for (ResourceLocation id : requested) {
            mutation m = mutation_registry.get(id);
            if (m == null) {
                source.sendFailure(Component.literal("Mutación desconocida: " + id));
                continue;
            }
            if (!ignoreRestrictions) {
                String failure = m.getApplyFailureReason(mob, working);
                if (failure != null) {
                    source.sendFailure(Component.literal("No se pudo aplicar " + id + ": " + failure));
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
            source.sendSuccess(() -> Component.literal("Mutaciones aplicadas a " + mob.getDisplayName().getString() + ": " + totalApplied), true);
        }
        return totalApplied;
    }

    private static void removeAllMutations(Mob mob, MobLegacyData data) {
        List<ResourceLocation> existing = new ArrayList<>(data.getMutations());
        if (existing.isEmpty()) {
            return;
        }
        data.clearMutations();
        for (ResourceLocation id : existing) {
            mutation m = mutation_registry.get(id);
            if (m != null) {
                m.onRemoval(mob);
            }
        }
    }

    private static void restoreMutations(Mob mob, MobLegacyData data, List<ResourceLocation> previous) {
        data.clearMutations();
        for (ResourceLocation id : previous) {
            mutation m = mutation_registry.get(id);
            if (m != null) {
                m.onApply(mob);
                data.addMutation(id);
            }
        }
    }
}
