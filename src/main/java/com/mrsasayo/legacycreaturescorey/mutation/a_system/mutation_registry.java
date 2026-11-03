package com.mrsasayo.legacycreaturescorey.mutation.a_system;

import com.mrsasayo.legacycreaturescorey.Legacycreaturescorey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Administración central de mutaciones cargadas desde JSON con un pequeño respaldo en código.
 */
public final class mutation_registry {
    private static final Map<Identifier, mutation> MUTATIONS = new LinkedHashMap<>();
    private static final Set<Identifier> DYNAMIC_MUTATION_IDS = new HashSet<>();

    private mutation_registry() {}

    public static void initialize() {
        Legacycreaturescorey.LOGGER.debug("Mutation registry initialized");
    }

    public static mutation get(Identifier id) {
        return MUTATIONS.get(id);
    }

    public static Collection<mutation> all() {
        return Collections.unmodifiableCollection(MUTATIONS.values());
    }

    public static void registerDynamicMutations(Collection<mutation> mutations) {
        for (Identifier id : DYNAMIC_MUTATION_IDS) {
            MUTATIONS.remove(id);
        }
        DYNAMIC_MUTATION_IDS.clear();

        Collection<mutation> source = mutations;
        boolean loadedFromJson = true;
        if (source == null || source.isEmpty()) {
            source = createFallbackMutations();
            if (source.isEmpty()) {
                Legacycreaturescorey.LOGGER.error("❌ No se pudieron registrar mutaciones: la carga JSON falló y no hay respaldo disponible.");
                return;
            }
            loadedFromJson = false;
            Legacycreaturescorey.LOGGER.warn("⚠️ No se cargaron mutaciones desde JSON; reinyectando {} mutaciones de respaldo.", source.size());
        }

        for (mutation m : source) {
            Identifier id = m.getId();
            if (MUTATIONS.containsKey(id) && !DYNAMIC_MUTATION_IDS.contains(id)) {
                Legacycreaturescorey.LOGGER.warn("⚠️ Reemplazando mutación existente {} mediante {}", id, loadedFromJson ? "JSON" : "respaldo");
            }
            MUTATIONS.put(id, m);
            DYNAMIC_MUTATION_IDS.add(id);
        }

        if (loadedFromJson) {
            Legacycreaturescorey.LOGGER.info("🧾 Mutaciones cargadas desde JSON: {}", source.size());
        } else {
            Legacycreaturescorey.LOGGER.info("✅ Se restauró el conjunto básico de mutaciones ({}) para mantener la jugabilidad.", source.size());
        }
    }

    private static Collection<mutation> createFallbackMutations() {
        return List.of(
            createExtraHealthMutation(),
            createSpeedMutation(),
            createKnockbackResistanceMutation(),
            createVenomStrikeMutation(),
            createCripplingBlowMutation(),
            createRegenerationMutation(),
            createDamageAuraMutation()
        );
    }

    private static mutation createExtraHealthMutation() {
        Identifier id = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "extra_health");
        return new abstract_mutation(id, mutation_type.PASSIVE_ATTRIBUTE, 2) {
            @Override
            public void onApply(LivingEntity entity) {
                AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
                if (attribute == null) {
                    return;
                }
                double newBase = attribute.getBaseValue() * 1.35D;
                attribute.setBaseValue(newBase);
                entity.setHealth((float) Math.min(entity.getHealth(), entity.getMaxHealth()));
            }

            @Override
            public void onRemove(LivingEntity entity) {
                AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
                if (attribute != null) {
                    attribute.setBaseValue(attribute.getBaseValue() / 1.35D);
                    entity.setHealth((float) Math.min(entity.getHealth(), entity.getMaxHealth()));
                }
            }
        };
    }

    private static mutation createSpeedMutation() {
        Identifier id = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "swiftness");
        return new abstract_mutation(id, mutation_type.PASSIVE_ATTRIBUTE, 1) {
            @Override
            public void onApply(LivingEntity entity) {
                AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attribute == null) {
                    return;
                }
                attribute.setBaseValue(attribute.getBaseValue() * 1.20D);
            }

            @Override
            public void onRemove(LivingEntity entity) {
                AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attribute != null) {
                    attribute.setBaseValue(attribute.getBaseValue() / 1.20D);
                }
            }
        };
    }

    private static mutation createKnockbackResistanceMutation() {
        Identifier id = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "fortitude");
        return new abstract_mutation(id, mutation_type.PASSIVE_ATTRIBUTE, 2) {
            @Override
            public void onApply(LivingEntity entity) {
                AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (attribute == null) {
                    return;
                }
                attribute.setBaseValue(attribute.getBaseValue() + 0.40D);
            }

            @Override
            public void onRemove(LivingEntity entity) {
                AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (attribute != null) {
                    attribute.setBaseValue(attribute.getBaseValue() - 0.40D);
                }
            }
        };
    }

    private static mutation createVenomStrikeMutation() {
        Identifier id = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "venom_strike");
        return new abstract_mutation(id, mutation_type.PASSIVE_ON_HIT, 2) {
            @Override
            public void onHit(LivingEntity attacker, LivingEntity target) {
                if (target.getEntityWorld().isClient()) {
                    return;
                }
                if (target.hasEffect(MobEffects.POISON)) {
                    return;
                }
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        };
    }

    private static mutation createCripplingBlowMutation() {
        Identifier id = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "crippling_blow");
        return new abstract_mutation(id, mutation_type.PASSIVE_ON_HIT, 1) {
            @Override
            public void onHit(LivingEntity attacker, LivingEntity target) {
                if (target.getEntityWorld().isClient()) {
                    return;
                }
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            }
        };
    }

    private static mutation createRegenerationMutation() {
        Identifier id = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "regrowth");
        return new abstract_mutation(id, mutation_type.ACTIVE, 2) {
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

    private static mutation createDamageAuraMutation() {
        Identifier id = Identifier.fromNamespaceAndPath(Legacycreaturescorey.MOD_ID, "destructive_aura");
        return new abstract_mutation(id, mutation_type.ACTIVE, 3) {
            @Override
            public void onTick(LivingEntity entity) {
                if (entity.getEntityWorld().isClient() || entity.age % 20 != 0) {
                    return;
                }

                ServerLevel world = (ServerLevel) entity.getEntityWorld();
                var damageSource = world.getDamageSources().magic();
                for (Player player : world.getPlayers()) {
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
