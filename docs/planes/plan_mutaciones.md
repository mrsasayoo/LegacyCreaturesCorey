**WARNING**

**Orden de Implementación según CSV**

El orden de refactorización e implementación de mutaciones seguirá **estrictamente** el orden definido en

docs/mutaciones.csv. Este archivo CSV es la

**fuente de verdad absoluta**

para:

- Orden de prioridad de implementación (líneas 199-...: **270 mutaciones**)
- Nombres exactos de mutaciones
- Categorías y clasificación
- Descripción detallada de comportamiento
- Niveles de dificultad y costos PM
- Restricciones de mob e incompatibilidades

Las primeras **270 mutaciones** del CSV (líneas 199-...) serán implementadas.

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
16. **Documentar:** Actualizar lista simple de mutaciones funcionando en task.md 
17. **Pasar al siguiente lote**

**IMPORTANT**

**Principio de Máxima Calidad:**

Cada lote debe estar **100% funcional** antes de pasar al siguiente. NO se avanza con bugs conocidos. El tiempo no es límite, la calidad sí.

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
