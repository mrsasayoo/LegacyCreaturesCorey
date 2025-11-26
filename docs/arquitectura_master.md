---
agent: agent
---
Actúa como un Ingeniero de Software Senior experto en Mods de Minecraft y Java. Tu objetivo es ejecutar la refactorización del proyecto 'LegacyCreaturesCorey' siguiendo ESTRICTAMENTE el siguiente plan de arquitectura. No te desvíes de las normas de nomenclatura (snake_case), la estructura de carpetas o el sistema de JSON 1:1 descritos a continuación:

# **Super Plan: Migración Arquitectónica de Mutaciones a Mapeo 1:1**

## **Resumen Ejecutivo**

Este plan establece la migración completa de la arquitectura de mutaciones hacia un mapeo estricto 1:1 entre archivos JSON y clases Java, adoptando la convención de nomenclatura `snake_case` para nombres de archivos y clases de acción.

**Objetivo Principal:** Refactorizar las primeras **197 mutaciones ya implementadas** bajo la nueva arquitectura 1:1, eliminando las clases multimodo genéricas y adoptando nomenclatura consistente en `snake_case`.

---

## **Decisiones Arquitectónicas Confirmadas**

**IMPORTANT**

**Convención de Nomenclatura Universal `snake_case`**

**TODOS** los archivos del proyecto migrarán a nomenclatura `snake_case`:

- Archivos Java existentes: `MutationAction.java` → `mutation_action.java`
- Clases de acción: `abyssal_armor_1_action.java`
- Mixins generales: `ram_damage_mixin.java`
- Archivos JSON: `abyssal_armor_1.json`

**IMPORTANT**

**Estructura JSON Simplificada**

El campo `description` NO debe mencionar la configuración de actions:

```
// ❌ INCORRECTO
"description": "Sus espinas infligen daño adicional. Los valores exactos se ven en la configuración de actions."
// ✅ CORRECTO
"description": "Sus espinas infligen daño adicional."

```

La explicación de cómo funcionan las configuraciones irá SOLO en la documentación, NO en los archivos JSON del código.

**WARNING**

**Orden de Implementación según CSV**

El orden de refactorización e implementación de mutaciones seguirá **estrictamente** el orden definido en

docs/mutaciones.csv. Este archivo CSV es la

**fuente de verdad absoluta**

para:

- Orden de prioridad de implementación (líneas 2-198: **197 mutaciones**)
- Nombres exactos de mutaciones
- Categorías y clasificación
- Descripción detallada de comportamiento
- Niveles de dificultad y costos PM
- Restricciones de mob e incompatibilidades

Las primeras **197 mutaciones** del CSV (líneas 2-198) serán refactorizadas primero, siguiendo el orden línea por línea.

---

## **Estrategia de Implementación por Lotes**

### **División de Lotes (7 lotes de 27 mutaciones)**

| **Lote** | **Líneas CSV** | **Mutaciones** | **Categorías Mixtas** | **Estado** |
| --- | --- | --- | --- | --- |
| **Lote 1** | 2-28 | 27 | Auras iniciales (requieren revisión completa) | 🔴 Pendiente |
| **Lote 2** | 29-55 | 27 | Auras + On-Hit iniciales | 🔴 Pendiente |
| **Lote 3** | 56-82 | 27 | On-Hit (chaos_touch, concussive_blow, critical, etc.) | 🔴 Pendiente |
| **Lote 4** | 83-109 | 27 | On-Hit (mining_fatigue, mortal_wound, nausea, etc.) | 🔴 Pendiente |
| **Lote 5** | 110-136 | 27 | On-Hit finales + Mob-Exclusive iniciales | 🔴 Pendiente |
| **Lote 6** | 137-163 | 27 | Mob-Exclusive (alphas_vengeance, ambusher, etc.) | 🔴 Pendiente |
| **Lote 7** | 164-198 | 35 | Mob-Exclusive finales (bastion_guard, etc.) | 🔴 Pendiente |

### **Flujo de Trabajo por Lote (Máxima Calidad)**

**Fase 1: Implementación (Sin Detenciones)**

1. Implementar las 27 mutaciones del lote EN ORDEN DEL CSV (categorías mezcladas según aparecen)
2. Crear archivos `.java` de acción con nomenclatura `snake_case`
3. Crear/actualizar archivos `.json` con estructura estandarizada
4. Crear mixins generales reutilizables si se necesitan (en `mixin/general/`)
5. Si hay errores durante implementación: **documentar y continuar** (NO detenerse)

**Fase 2: Corrección (Rondas de Arreglo)** 6. **Compilar:** `./gradlew clean build` 7. Revisar errores de compilación 8. **Ronda 1 de corrección:** Arreglar errores críticos que impiden compilación 9. **Compilar nuevamente:** `./gradlew build` 10. **Ejecutar:** `./gradlew runClient` 11. **Probar en juego:** - `/corey reload` - `/corey mutation list` - Verificar que las 27 mutaciones cargan - Invocar 3-5 mobs con mutaciones del lote - Verificar comportamiento básico 12. **Revisar logs:** `run/logs/latest.log` + logs por categoría 13. **Ronda 2 de corrección:** Arreglar errores funcionales detectados en juego 14. **Repetir pruebas** hasta que TODO el lote funcione correctamente

**Fase 3: Mantenimiento** 15. **Commit:** Hacer commit a rama principal con mensaje descriptivo - Formato: `feat(lote-X): implementar mutaciones [nombre_primera] a [nombre_ultima]` 16. **Documentar:** Actualizar lista simple de mutaciones funcionando en task.md 17. **Pasar al siguiente lote**

**IMPORTANT**

**Principio de Máxima Calidad:**

Cada lote debe estar **100% funcional** antes de pasar al siguiente. NO se avanza con bugs conocidos. El tiempo no es límite, la calidad sí.

---

## **Sistema de Configuración Externa (Adición al Archivo Existente)**

**NOTE**

**Archivo Existente con Múltiples Sistemas**

El archivo `run/config/legacycreaturescorey.json` **ya existe** y es utilizado por otros sistemas del mod (como Anti-Farm). La configuración de mutaciones se agregará como una **sección adicional** a este archivo existente, NO se sobrescribirá el archivo completo.

### **Sección a Agregar: `run/config/legacycreaturescorey.json`**

```
{
  "antifarm": {
    ...configuración existente del sistema antifarm...
  },

  "mutation_system": {
    "max_mutations_per_mob": 3,
    "mutation_point_budget": 100,
    "allow_incompatible_mutations": false,
    "enable_debug_logging": false
  },

  "category_toggle": {
    "passive": true,
    "on_hit": true,
    "mob_exclusive": true,
    "auras": true,
    "on_being_hit": true,
    "on_death": true,
    "synergy": true,
    "terrain": true
  },

  "weighting_system": {
    "weighting_weight": 0
  },

  "cost_system": {
    "general_cost_reduction": 0.0,
    "standardized_cost_difficulty": {
      "weak": null,
      "intermediate": null,
      "strong": null
    }
  },

  "performance": {
    "passive_tick_interval": 20,
    "aura_check_interval": 20
  }
}

```

**Explicación de Campos:**

| **Campo** | **Tipo** | **Función** | **Ejemplo** |
| --- | --- | --- | --- |
| `category_toggle."[categoria]"` | Boolean | Deshabilita TODA una categoría (sobrescribe `enabled` individual de JSON) | `"auras": false` → Ninguna aura se aplicará |
| `weighting_weight` | Float (0-1) | `0` = Pesos originales del CSV. `1` = Probabilidad uniforme (débil = intermedio = difícil) | `0.5` = 50% hacia uniformidad |
| `general_cost_reduction` | Float (0-1) | Multiplica el costo PM de TODAS las mutaciones | `0.5` = Todas cuestan 50% menos |
| `standardized_cost_difficulty."weak"` | Integer o null | Si NO es `null`, sobrescribe el costo de TODAS las mutaciones débiles | `5` = Todas las débiles cuestan 5 PM |
| `passive_tick_interval` | Integer | Cada cuántos ticks se ejecutan efectos pasivos/auras | `20` = 1 vez por segundo |

---

## **Sistema de Logging Multi-Nivel**

### **Configuración de Logs**

**Archivo Principal:** `run/logs/latest.log`

- Nivel: **INFO** (eventos importantes + errores + advertencias)
- Idioma: **Español**
- Contiene: Toda la información necesaria para debugging

**Logs por Categoría:**

- `run/logs/mutations_aura.log` - Solo eventos de auras
- `run/logs/mutations_on_hit.log` - Solo eventos on-hit
- `run/logs/mutations_mob_exclusive.log` - Solo exclusivas de mob
- `run/logs/mutations_passive.log` - Solo pasivas
- `run/logs/mutations_on_being_hit.log` - Solo on-being-hit
- `run/logs/mutations_on_death.log` - Solo on-death
- `run/logs/mutations_synergy.log` - Solo sinergias
- `run/logs/mutations_debug.log` - Debug técnico general

**Ejemplo de Mensajes (Español):**

```
[INFO] [LegacyCreaturesCorey] Cargando mutación: abyssal_armor_1
[WARN] [LegacyCreaturesCorey] Mutación corruption_aura_2 incompatible con deep_darkness_aura_3 detectada en Elder Guardian
[ERROR] [LegacyCreaturesCorey] Error al aplicar mutación aerial_maneuvers_3: NullPointerException en método on_tick_passive

```

---

## **Hooks y Performance**

### **Sistema de Tick Throttling para Auras**

**Configuración:** `passive_tick_interval: 20` (1 vez/segundo por defecto)

```
// Pseudocódigo
private int tickCounter = 0;
privatefinal int TICK_INTERVAL = ConfigManager.getPassiveTickInterval();// 20
@Override
public void on_tick_passive(LivingEntity entity) {
    tickCounter++;
    if (tickCounter >= TICK_INTERVAL) {
        tickCounter = 0;
// Ejecutar lógica de aura
        checkAuraRadius(entity);
        applyAuraEffects(entity);
    }
}

```

**Características:**

- Verificación de radio de efecto en CADA activación del intervalo configurado
- Sin límite de mutaciones pasivas por mob
- Frecuencia configurable desde archivo externo

---

## **Estructura de Paquetes Definitiva**

```
src/main/java/com/mrsasayo/legacycreaturescorey/
├── mutation/
│   ├── a_system/                    # Providers, managers (aparece primero alfabéticamente)
│   │   ├── mutation_provider.java
│   │   ├── incompatibility_manager.java
│   │   └── weighting_calculator.java
│   ├── action/
│   │   ├── auras/
│   │   │   ├── corruption_aura_1_action.java
│   │   │   └── ...
│   │   ├── mob_exclusive/
│   │   ├── on_hit/
│   │   ├── on_being_hit/
│   │   ├── on_death/
│   │   ├── passive/
│   │   ├── synergy/
│   │   └── terrain/
│   ├── data/
│   │   └── mutation_data_loader.java
│   └── util/                        # Utilidades específicas de mutaciones
│       ├── damage_calculator.java
│       ├── distance_verifier.java
│       └── effect_applier.java
├── mixin/
│   └── general/                     # Mixins reutilizables
│       ├── living_entity_hooks_mixin.java
│       ├── ram_damage_mixin.java
│       └── ...
└── command/
    └── corey/

```

---

## **FASE 1: Refactorización Masiva (El Gran Desglose)**

Esta fase migra todas las mutaciones según el orden del CSV a la arquitectura 1:1, adoptando la nomenclatura `snake_case` estricta.

**Convención de Nomenclatura de Archivos (CRÍTICO):**

| **Tipo de Archivo** | **Patrón de Nomenclatura** | **Ejemplo** |
| --- | --- | --- |
| **Configuración JSON** | `[nombre_mutacion]_[nivel].json` | `abyssal_armor_1.json` |
| **Lógica de Acción** | `[nombre_mutacion]_[nivel]_action.java` | `abyssal_armor_1_action.java` |
| **Mixin (si necesario)** | `[nombre_mutacion]_[nivel]_mixin.java` | `abyssal_armor_1_mixin.java` |

**IMPORTANT**

**Todos los nombres de archivo deben usar `snake_case` (minúsculas con guiones bajos)**. Esta es la convención obligatoria para todo el proyecto.

### **1.1. Desglose de Mutaciones Mob-Exclusive**

**Estrategia:** Por cada clase multimodo, crear 3 archivos independientes (niveles 1, 2, 3).

| **Clase Multimodo Legacy** | **Clases 1:1 Resultantes** | **JSON Asociados** |
| --- | --- | --- |
| `AbyssalArmorAction.java` | `abyssal_armor_1_action.javaabyssal_armor_2_action.javaabyssal_armor_3_action.java` | `abyssal_armor_1.jsonabyssal_armor_2.jsonabyssal_armor_3.json` |
| `AlphasVengeanceAction.java` | `alphas_vengeance_1_action.javaalphas_vengeance_2_action.javaalphas_vengeance_3_action.java` | `alphas_vengeance_1.jsonalphas_vengeance_2.jsonalphas_vengeance_3.json` |
| `AmbusherAction.java` | `ambusher_1_action.javaambusher_2_action.javaambusher_3_action.java` | `ambusher_1.jsonambusher_2.jsonambusher_3.json` |
| ... | ... | ... |

**Proceso de Migración:**

1. Extraer lógica específica de cada nivel (`level 1`, `level 2`, `level 3`)
2. Crear nueva clase `snake_case` implementando `mutation_action`
3. Implementar todos los hooks (incluso si están vacíos)
4. Verificar que el JSON apunte a la nueva clase
5. Probar en juego la funcionalidad específica
6. **Eliminar** clase multimodo original

---

### **1.2. Desglose de Mutaciones On-Hit**

| **Clase Multimodo Legacy** | **Clases 1:1 Resultantes** |
| --- | --- |
| `ChaosTouchOnHitAction.java` | `chaos_touch_1_action.javachaos_touch_2_action.javachaos_touch_3_action.java` |
| `BleedingOnHitAction.java` | `bleeding_1_action.javableeding_2_action.javableeding_3_action.java` |
| `CriticalDamageOnHitAction.java` | `critical_damage_1_action.javacritical_damage_2_action.javacritical_damage_3_action.java` |
| ... | ... |

---

### **1.3. Desglose de Mutaciones Aura**

**Estado Actual:** Parcialmente migrado a 1:1, **requiere revisión completa**.

**Acción Requerida:**

- Identificar auras aún en formato multimodo
- Migrar a formato 1:1 `snake_case`
- Asegurar implementación de nuevos hooks
- Verificar estructura JSON completa
- Validar tick throttling implementado

---

### **1.4. Desglose de Mutaciones Passive**

**NOTE**

**Comportamiento Especial de Mutaciones Pasivas**

Las mutaciones pasivas **solo se aplican cuando el mob aparece en el mundo**. Su función es **modificar atributos permanentes** del mob (ej: velocidad, armadura, salud máxima) en lugar de ejecutar lógica activa durante el combate.

| **Categoría** | **Acción** |
| --- | --- |
| Passivas multimodo existentes | Migrar a 1:1 siguiendo patrón `[nombre]_[nivel]_action.java` |
| Nuevas passivas | Implementar directamente en formato 1:1 |
| Aplicación | Solo en `spawn` del mob, modificando atributos de `LivingEntity` |

---

### **1.5. Actualización Masiva de JSON**

**Estructura JSON Estandarizada:**

Todos los archivos JSON de mutación DEBEN seguir esta estructura exacta:

```
{
  "enabled": true,
  "id": "legacycreaturescorey:mob_exclusive/abyssal_armor_1",
  "type": "mob_exclusive",
  "display_name": "Abyssal Armor I",
  "cost": 6,
  "weight": 60,
  "description": "Sus espinas infligen daño adicional.",
  "actions": {
    "damage_bonus": 1.0,
    "other_parameters": "según la mutación"
  },
  "entity_types": ["minecraft:elder_guardian"],
  "incompatible_with": []
}

```

**Campos Requeridos:**

| **Campo** | **Tipo** | **Descripción** |
| --- | --- | --- |
| `enabled` | Boolean | `true` o `false` para habilitar/deshabilitar la mutación |
| `id` | String | Identificador único con ruta: `categoria/nombre_nivel` |
| `type` | String | Categoría de mutación (ver lista abajo) |
| `display_name` | String | Nombre visible en el juego |
| `cost` | Integer | Costo en Puntos de Mutación (PM) |
| `weight` | Integer | Probabilidad de selección (valor más alto = más probable) |
| `description` | String | Descripción general (sin valores específicos) |
| `actions` | Object | Configuración específica de la mutación |
| `entity_types` | Array | Lista de tipos de entidad compatibles |
| `incompatible_with` | Array | Lista de IDs de mutaciones incompatibles |

**Categorías Válidas (`type`):**

- `auras`
- `on_hit`
- `mob_exclusive`
- `on_being_hit`
- `on_death`
- `passive`
- `synergy`
- `terrain`

**Tareas:**

- Script de validación para verificar estructura JSON estandarizada
- Migrar TODOS los JSON existentes a la nueva estructura
- Verificar carga correcta con `/corey reload`

---

## **Nomenclatura y Constantes**

### **Constantes: UPPER_SNAKE_CASE**

```
publicclass corruption_aura_1_actionimplements mutation_action {
// Constantes globales
privatestaticfinal int DAMAGE_AMOUNT = 1;
privatestaticfinal int EFFECT_RADIUS = 3;
privatestaticfinal int DAMAGE_INTERVAL_TICKS = 80;// 4 segundos

// Estos valores se pueden sobrescribir desde JSON
}

```

### **Enums: UPPER_SNAKE_CASE**

```
publicenum MUTATION_CATEGORY {
    AURAS,
    ON_HIT,
    MOB_EXCLUSIVE,
    ON_BEING_HIT,
    ON_DEATH,
    PASSIVE,
    SYNERGY,
    TERRAIN
}
publicenum DIFFICULTY_LEVEL {
    WEAK,
    INTERMEDIATE,
    STRONG
}

```

**Nota:** Las constantes se definen en cada clase para ser configurables desde el JSON asignado a esa mutación específica.

---

## **Validación de entity_types**

### **Reglas de Corrección**

1. **Si CSV dice restricción de mob pero JSON tiene `entity_types: []`:**
    - Agregar el mob mencionado en el CSV al array
    - Usar IDs de Minecraft vanilla (ej: `minecraft:elder_guardian`)
2. **Formato de IDs:**
    - SIEMPRE usar IDs vanilla de Minecraft
    - NO usar IDs del mod
    - NO usar wildcards (cada tipo de esqueleto se lista individualmente)

**Ejemplo de Corrección:**

```
// CSV: "Restricción de Mob: Guardián Anciano"
// JSON actual:
{
  "entity_types": []
}
// JSON corregido:
{
  "entity_types": ["minecraft:elder_guardian"]
}

```

---

## **Git Workflow**

### **Estrategia de Commits**

- **NO** usar branches por lote
- Trabajar directamente en rama principal
- **Commit por cada bloque implementado correctamente**

**Formato de Commits:**

```
# Después de completar implementación de lote (fase 1)
git add .
git commit -m "feat(lote-1): implementar lógica mutaciones corruption_aura_1 a stasis_field_3"
# Después de corrección exitosa (fase 2)
git add .
git commit -m "fix(lote-1): corregir errores de compilación y funcionalidad en auras"
# Después de pruebas exitosas (fase 3)
git add .
git commit -m "test(lote-1): validar funcionamiento de 27 mutaciones del lote 1"

```

---

## **Revisión de Auras Existentes (Lote 1)**

### **Aspectos a Revisar**

**TODOS** los siguientes aspectos deben revisarse para cada aura:

1. **Nomenclatura:** Archivos y clases deben seguir `snake_case`
2. **Estructura JSON:** Debe tener TODOS los campos requeridos
3. **Hooks:** Implementación correcta de `on_tick_passive` con throttling
4. **Performance:** Verificación optimizada de radio
5. **Lógica:** Cumplimiento exacto de descripción en CSV
6. **Configurabilidad:** Constantes extraídas y configurables desde JSON

**Principio:** Se actualiza **de todos modos** aunque funcione correctamente, para garantizar consistencia arquitectónica.

Las auras se revisan en el **Lote 1** junto con las demás mutaciones, siguiendo el orden del

mutaciones.csv.

---

## **Sistema de Incompatibilidades**

### **Validación Activa**

```
public boolean canAssignMutation(LivingEntity mob, Mutation mutation) {
// 1. Verificar que el tipo de mob está permitido
    if (!isEntityTypeAllowed(mob, mutation)) {
        LOGGER.warn("Mutación {} no permitida para mob {}",
                    mutation.getId(), mob.getType());
        return false;
    }

// 2. Verificar incompatibilidades
    if (hasIncompatibleMutation(mob, mutation)) {
        String incompatible = getIncompatibleMutationId(mob, mutation);
        LOGGER.warn("Mutación incompatible detectada: {} conflicto con {}",
                    mutation.getId(), incompatible);
        return false;
    }

    return true;
}

```

**Políticas:**

- Usuario **NO puede** agregar/modificar incompatibilidades desde config
- Las incompatibilidades están definidas SOLO en archivos JSON individuales
- Sistema valida activamente y rechaza asignaciones incompatibles

---

## **Plan de Verificación**

### **Pruebas Manuales en Juego**

| **Categoría** | **Mutaciones a Probar** | **Método de Verificación** |
| --- | --- | --- |
| **Auras** | `aerial_maneuvers_*`, `alpha_presence_*` | Verificar área de efecto y efectos aplicados |
| **Mob-Exclusive** | `abyssal_armor_*`, `alphas_vengeance_*` | Invocar mobs específicos y verificar comportamiento |
| **On-Hit** | `chaos_touch_*`, `bleeding_*` | Atacar y verificar efectos aplicados |
| **Passive** | (según las identificadas) | Verificar efectos continuos |
| **Synergy** | (según configuradas) | Verificar cambios de comportamiento según contexto (ej: mob en agua, cerca de aliados, etc.) |

---

## **✅ Plan Completo y Listo para Implementación**

Este plan contiene:

- ✅ **197 mutaciones** prioritarias definidas
- ✅ **7 lotes** de 27 mutaciones cada uno
- ✅ **Flujo de trabajo detallado** (3 fases por lote)
- ✅ **Nomenclatura universal** `snake_case`
- ✅ **Sistema de configuración** como adición al archivo existente
- ✅ **Logging multinivel** en español
- ✅ **Estructura de paquetes** con ruta corregida
- ✅ **Ejemplos completos** de cada categoría
- ✅ **Validaciones** y manejo de incompatibilidades
- ✅ **Git workflow** definido
- ✅ **Principio de máxima calidad**