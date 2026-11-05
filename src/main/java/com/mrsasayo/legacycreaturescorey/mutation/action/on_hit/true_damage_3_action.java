package com.mrsasayo.legacycreaturescorey.mutation.action.on_hit;

import com.mrsasayo.legacycreaturescorey.mutation.util.mutation_action_config;

import java.util.List;

/**
 * true_damage_3 «Herida Verdadera»: el golpe verdadero DEJA LA PUERTA ABIERTA —
 * marca al jugador con «Expuesto» II (+20% de daño recibido por 3 s, efecto
 * compartido con critical — gramática unificada). El self-downtime sube a 3 s
 * (vs 1.5/2.5 de los niveles 1/2).
 */
public final class true_damage_3_action extends true_damage_base_action {
    public true_damage_3_action(mutation_action_config config) {
        super(config, 0.08D, List.of(), 60);
    }
}
