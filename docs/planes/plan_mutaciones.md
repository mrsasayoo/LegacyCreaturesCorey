**WARNING**

**Orden de Implementación según CSV**

> **Estado actualizado (2026-11-03, post-port 26.3):** los **Lotes 1-7 (líneas 2-198) están
> implementados y verificados en juego** — el Lote 7 cerró 57/57 al registrarse
> `aquatic_stalker`/`arctic_fortitude` en el validador (verificado en mundo el 2026-11-03).
> El estado por mutación está en la columna `implementada_en_el_mod` de `docs/mutaciones.csv`
> (SI=197 / NO=949, verificada contra el validador runtime del mod). A partir del Lote 8
> continuar con Último Aliento (líneas 199+).

El orden de refactorización e implementación de mutaciones seguirá **estrictamente** el orden definido en

docs/mutaciones.csv. Este archivo CSV es la

**fuente de verdad absoluta**

para:

- Orden de prioridad de implementación (líneas 199-...: **949 mutaciones**)
- Nombres exactos de mutaciones
- Categorías y clasificación
- Descripción detallada de comportamiento
- Niveles de dificultad y costos PM
- Restricciones de mob e incompatibilidades

Las primeras **949 mutaciones** del CSV (líneas 199-...) serán implementadas.

---

## **Estrategia de Implementación por Lotes**

### **División de Lotes (7 lotes de 27 mutaciones)**

| **Lote** | **Líneas CSV** | **Mutaciones** | **Categorías Mixtas** | **Estado** |
| --- | --- | --- | --- | --- |
| **Lote 1** | 2-28 | 27 | Auras iniciales (requieren revisión completa) | ✅ Completado |
| **Lote 2** | 29-55 | 27 | Auras + On-Hit iniciales | ✅ Completado |
| **Lote 3** | 56-82 | 27 | On-Hit (chaos_touch, concussive_blow, critical, etc.) | ✅ Completado |
| **Lote 4** | 83-109 | 27 | On-Hit (mining_fatigue, mortal_wound, nausea, etc.) | ✅ Completado |
| **Lote 5** | 110-136 | 27 | On-Hit finales + Mob-Exclusive iniciales | ✅ Completado |
| **Lote 6** | 137-163 | 27 | Mob-Exclusive (alphas_vengeance, ambusher, etc.) | ✅ Completado |
| **Lote 7** | 164-198 | 35 | Mob-Exclusive finales (bastion_guard, etc.) | ✅ 57/57 (aquatic_stalker y arctic_fortitude implementadas y verificadas 2026-11-03) |

### **Flujo de Trabajo por Lote (Máxima Calidad)**

**Fase 1: Implementación (Sin Detenciones)**

1. Implementar las 27 mutaciones del lote EN ORDEN DEL CSV (categorías mezcladas según aparecen)
2. Crear archivos `.java` de acción con nomenclatura `snake_case`
3. Crear/actualizar archivos `.json` con estructura estandarizada
4. Crear mixins generales reutilizables si se necesitan (en `mixin/general/`)
5. Si hay errores durante implementación: **arreglarlos sobre la marcha** (NO detenerse)

**Fase 2: Corrección (Rondas de Arreglo)** 
6. **Compilar:** `./gradlew clean build` 
7. Revisar errores de compilación 
8. **Ronda 1 de corrección:** Arreglar errores críticos que impiden compilación 
9. **Compilar nuevamente:** `./gradlew build` 
10. **Ejecutar:** `./gradlew runClient`(siempre avisar antes de ejecutar este comando)
11. **Probar en juego:** - `/corey reload` - `/corey mutation list` - Verificar que las 27 mutaciones cargan - Invocar 3-5 mobs con mutaciones del lote - Verificar comportamiento básico 
12. **Revisar logs:** `run/logs/latest.log` + logs por categoría 
13. **Ronda 2 de corrección:** Arreglar errores funcionales detectados en juego 
14. **Repetir pruebas** hasta que TODO el lote funcione correctamente

**Fase 3: Mantenimiento** 
15. **Commit:** Hacer commit a rama principal con mensaje descriptivo - Formato: `feat(lote-X): implementar mutaciones [nombre_primera] a [nombre_ultima]` 
16. **Documentar:** marcar las mutaciones del lote como `SI` en la columna `implementada_en_el_mod` de `docs/mutaciones.csv` (única fuente de tracking — decisión 2026-11-03) 
17. **Pasar al siguiente lote**

**IMPORTANT**

**Principio de Máxima Calidad:**

Cada lote debe estar **100% funcional** antes de pasar al siguiente. NO se avanza con bugs conocidos. El tiempo no es límite, la calidad sí.

**Estructura JSON Estandarizada:**

Todos los archivos JSON de mutación DEBEN seguir la estructura real del mod (esquema vigente,
verificado contra los 197 JSON existentes y el validador al cargar los datapacks):

```
{
    "enabled": true,
    "id": "legacycreaturescorey:mob_exclusive/abyssal_armor_1",
    "type": "MOB_EXCLUSIVE",
    "cost": 6,
    "display_name": {
        "literal": "Armadura Abisal I"
    },
    "description": {
        "literal": "Sus espinas infligen daño mágico adicional."
    },
    "weight": 60,
    "actions": {
        "bonus_magic_damage": 1.0
    },
    "entity_types": [
        "minecraft:elder_guardian"
    ],
    "incompatible_with": []
}
```

**Notas del esquema real (2026-11-03):**

- `display_name` y `description` son objetos `{ "literal": "..." }`, NO strings.
- El campo `type` aparece en los JSON reales tanto en MAYÚSCULAS (`MOB_EXCLUSIVE`,
  `PASSIVE_ATTRIBUTE`) como en minúsculas (`on_hit`, `auras`, `passive`) según la carpeta:
  copiar el valor de un JSON existente de la misma carpeta.
- `actions` admite DOS formas: objeto simple de parámetros (ejemplo arriba; la clase de acción
  lo consume vía `mutation_action_config`) o array tipado de 1 elemento con `"type"` de acción
  (p. ej. `aquatic_stalker_1.json`: `[ { "type": "aquatic_stalker", "mode": "SWIFT_SWIM" } ]`).
- Un tipo de acción nuevo DEBE registrarse en el validador (`ACTION_VALIDATORS` de
  `mutation_schema_validator`) y en el switch `loadSpecificAction` de `mutation_data_loader`
  (además de crear su clase de acción en `mutation/action/`).

**Campos Requeridos:**

| **Campo** | **Tipo** | **Descripción** |
| --- | --- | --- |
| `enabled` | Boolean | `true` o `false` para habilitar/deshabilitar la mutación |
| `id` | String | Identificador único con ruta: `categoria/nombre_nivel` |
| `type` | String | Categoría de mutación, en el formato que usen los JSON de la misma carpeta (ver notas del esquema) |
| `display_name` | Object | `{ "literal": "Nombre visible" }` — NO string plano |
| `cost` | Integer | Costo en Puntos de Mutación (PM) |
| `weight` | Integer | Probabilidad de selección (valor más alto = más probable) |
| `description` | Object | `{ "literal": "Descripción general" }` — NO string plano |
| `actions` | Object o Array | Objeto de parámetros simple O array tipado de 1 acción con `"type"` (ver notas del esquema) |
| `entity_types` | Array | Lista de tipos de entidad compatibles |
| `incompatible_with` | Array | Lista de IDs de mutaciones incompatibles |

**Categorías Válidas (`type`) — valores reales observados en los 197 JSON (2026-11-03):**

- `MOB_EXCLUSIVE` (carpeta `mob_exclusive/`)
- `on_hit` (carpeta `on_hit/`)
- `auras` (carpeta `auras/`)
- `passive` y `PASSIVE_ATTRIBUTE` (carpeta `passive/`)
- `on_being_hit`, `on_death`, `synergy`, `terrain` — aún sin JSON en el repo (lotes pendientes)

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

## **Validación de entity_types**

### **Reglas de Corrección**

1. **Si CSV dice restricción de mob pero JSON tiene `entity_types: []`:**
    - Agregar el mob mencionado en el CSV al array
    - Usar IDs de Minecraft vanilla (ej: `minecraft:elder_guardian`)
2. **Formato de IDs:**
    - SIEMPRE usar IDs vanilla de Minecraft
    - NO usar IDs del mod
    - NO usar wildcards (cada tipo de esqueleto se lista individualmente)
