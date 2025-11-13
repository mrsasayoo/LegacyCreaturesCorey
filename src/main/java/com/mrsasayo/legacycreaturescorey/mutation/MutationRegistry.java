package com.mrsasayo.legacycreaturescorey.mutation;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registro temporal de mutaciones hardcodeadas hasta implementar carga por JSON.
 */
public final class MutationRegistry {
    private static final Map<Identifier, Mutation> MUTATIONS = new LinkedHashMap<>();
    private static final Set<Identifier> DYNAMIC_MUTATION_IDS = new HashSet<>();

    private MutationRegistry() {}

    public static void initialize() {
        register(createExtraHealthMutation());
        register(createSpeedMutation());
        register(createKnockbackResistanceMutation());
        register(createVenomStrikeMutation());
        register(createCripplingBlowMutation());
        register(createRegenerationMutation());
        register(createDamageAuraMutation());
        Legacycreaturescorey.LOGGER.info("✅ Mutaciones hardcodeadas registradas: {}", MUTATIONS.size());
    }

    public static Mutation get(Identifier id) {
        return MUTATIONS.get(id);
    }

    public static Collection<Mutation> all() {
        return Collections.unmodifiableCollection(MUTATIONS.values());
    }

    private static void register(Mutation mutation) {
        Identifier id = mutation.getId();
        if (MUTATIONS.containsKey(id)) {
            throw new IllegalStateException("Duplicate mutation id: " + id);
        }
        MUTATIONS.put(id, mutation);
    }

    public static void registerDynamicMutations(Collection<Mutation> mutations) {
        for (Identifier id : DYNAMIC_MUTATION_IDS) {
            MUTATIONS.remove(id);
        }
        DYNAMIC_MUTATION_IDS.clear();

        for (Mutation mutation : mutations) {
            Identifier id = mutation.getId();
            if (MUTATIONS.containsKey(id) && !DYNAMIC_MUTATION_IDS.contains(id)) {
                Legacycreaturescorey.LOGGER.warn("⚠️ Reemplazando mutación estática {} mediante JSON", id);
            }
            MUTATIONS.put(id, mutation);
            DYNAMIC_MUTATION_IDS.add(id);
        }

        if (!mutations.isEmpty()) {
            Legacycreaturescorey.LOGGER.info("🧾 Mutaciones cargadas desde JSON: {}", mutations.size());
        }
    }

    private static Mutation createExtraHealthMutation() {
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "extra_health");
        return new AbstractMutation(id, MutationType.PASSIVE_ATTRIBUTE, 2) {
            @Override
            public void onApply(LivingEntity entity) {
                EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (attribute == null) {
                    return;
                }
                double newBase = attribute.getBaseValue() * 1.35D;
                attribute.setBaseValue(newBase);
                entity.setHealth((float) Math.min(entity.getHealth(), entity.getMaxHealth()));
            }

            @Override
            public void onRemove(LivingEntity entity) {
                EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (attribute != null) {
                    attribute.setBaseValue(attribute.getBaseValue() / 1.35D);
                    entity.setHealth((float) Math.min(entity.getHealth(), entity.getMaxHealth()));
                }
            }
        };
    }

    private static Mutation createSpeedMutation() {
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "swiftness");
        return new AbstractMutation(id, MutationType.PASSIVE_ATTRIBUTE, 1) {
            @Override
            public void onApply(LivingEntity entity) {
                EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                if (attribute == null) {
                    return;
                }
                attribute.setBaseValue(attribute.getBaseValue() * 1.20D);
            }

            @Override
            public void onRemove(LivingEntity entity) {
                EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                if (attribute != null) {
                    attribute.setBaseValue(attribute.getBaseValue() / 1.20D);
                }
            }
        };
    }

    private static Mutation createKnockbackResistanceMutation() {
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "fortitude");
        return new AbstractMutation(id, MutationType.PASSIVE_ATTRIBUTE, 2) {
            @Override
            public void onApply(LivingEntity entity) {
                EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
                if (attribute == null) {
                    return;
                }
                attribute.setBaseValue(attribute.getBaseValue() + 0.40D);
            }

            @Override
            public void onRemove(LivingEntity entity) {
                EntityAttributeInstance attribute = entity.getAttributeInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
                if (attribute != null) {
                    attribute.setBaseValue(attribute.getBaseValue() - 0.40D);
                }
            }
        };
    }

    private static Mutation createVenomStrikeMutation() {
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "venom_strike");
        return new AbstractMutation(id, MutationType.PASSIVE_ON_HIT, 2) {
            @Override
            public void onHit(LivingEntity attacker, LivingEntity target) {
                if (target.getEntityWorld().isClient()) {
                    return;
                }
                if (target.hasStatusEffect(StatusEffects.POISON)) {
                    return;
                }
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0));
            }
        };
    }

    private static Mutation createCripplingBlowMutation() {
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "crippling_blow");
        return new AbstractMutation(id, MutationType.PASSIVE_ON_HIT, 1) {
            @Override
            public void onHit(LivingEntity attacker, LivingEntity target) {
                if (target.getEntityWorld().isClient()) {
                    return;
                }
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1));
            }
        };
    }

    private static Mutation createRegenerationMutation() {
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "regrowth");
        return new AbstractMutation(id, MutationType.ACTIVE, 2) {
            @Override
            public void onTick(LivingEntity entity) {
                if (entity.getEntityWorld().isClient() || entity.age % 40 != 0) {
                    return;
                }
                if (entity.getHealth() < entity.getMaxHealth()) {
                    entity.heal(1.5F);
                }
            }
        };
    }

    private static Mutation createDamageAuraMutation() {
        Identifier id = Identifier.of(Legacycreaturescorey.MOD_ID, "destructive_aura");
        return new AbstractMutation(id, MutationType.ACTIVE, 3) {
            @Override
            public void onTick(LivingEntity entity) {
                if (entity.getEntityWorld().isClient() || entity.age % 20 != 0) {
                    return;
                }

                ServerWorld world = (ServerWorld) entity.getEntityWorld();
                var damageSource = world.getDamageSources().magic();
                for (PlayerEntity player : world.getPlayers()) {
                    if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                        continue;
                    }
                    if (player.squaredDistanceTo(entity) > 9.0D) {
                        continue;
                    }
                    player.damage(world, damageSource, 2.0F);
                }
            }
        };
    }
}
