# API REFERENCE - Legacy Creatures Corey

> Actualizado para **MC 26.3** (nombres oficiales de Mojang, Fabric API 0.165.0+26.3, Java 25).
> Todas las firmas listadas fueron verificadas contra el código compilado (commit `ed3ae28`).

## 📦 Paquete Público
`com.mrsasayo.legacycreaturescorey.core.api`

### CoreyAPI
Helper estático para que otros mods interactúen con el ecosistema Corey sin depender de clases internas.

#### Métodos

##### `Optional<Mob> spawnCategorizedMob(ServerLevel world, BlockPos pos, EntityType<? extends Mob> type, MobTier tier)`
- **Descripción:** genera un mob del tipo indicado, lo promueve al `tier` solicitado y aplica mutaciones por defecto.
- **Retorno:** `Optional` con la entidad creada (vacío si falló el spawn).
- **Errores:** devuelve `Optional.empty()` cuando `world/type/pos` son nulos o el spawn falla.

##### `Optional<Mob> spawnCategorizedMob(..., List<Identifier> forcedMutations)`
- **Descripción:** idéntico al anterior, pero permite forzar una lista de mutaciones exacta.
- **Notas:** si `forcedMutations` no está vacío se limpian las mutaciones que asignaría `TierManager`.

##### `Optional<MobTier> getTier(Mob mob)`
- Recupera el tier actual desde `MobLegacyData`. Vuelve vacío si la entidad no posee el attachment.

##### `List<Identifier> getMutations(Mob mob)`
- Devuelve las mutaciones registradas para el mob. Nunca `null`.

##### `boolean registerSynergyProvider(SynergyProvider provider)`
- Registra un proveedor externo en `SynergyManager`.
- Retorna `false` si el módulo asociado no está presente o si `provider.validate()` falla.

##### `List<SynergyStatus> getSynergyStatuses()`
- Devuelve la lista inmutable de estados (habilitado, ausente, deshabilitado) para todas las sinergias detectadas.

### LegacyCreaturesTierApi
Expone utilidades centradas en tiers para mods que no necesitan el resto de `CoreyAPI`.

#### Métodos
- `EnumSet<MobTier> getAllowedTiers(EntityType<?>)`: copia inmutable de los tiers habilitados por datapack.
- `boolean isTierAllowed(EntityType<?>, MobTier)`: helper directo para comprobaciones rápidas.
- `Optional<MobTier> getTier(Mob)`: obtiene el tier ya asignado (vacío si está en `NORMAL`).
- `void forceTier(Mob, MobTier, boolean assignDefaultMutations)`: atajo oficial al `TierManager`.

> Nota: `getTier` refleja exactamente el estado del attachment `MobLegacyData`; regresar `Optional.empty()` significa que el mob sigue en `NORMAL` o todavía no fue categorizado.

### AntiFarmDashboardApi
Pensado para paneles que quieran mostrar calor anti-farm.

- `Map<Long, ChunkActivityData> getChunkActivitySnapshot(MinecraftServer)`: snapshot completo (chunk key → datos).
- `Optional<ChunkActivityData> getChunkActivity(MinecraftServer, ChunkPos)`: detalle puntual.
- `boolean isChunkBlocked(MinecraftServer, ChunkPos)`: comprueba si el chunk sigue vetado.

## 🔌 Interfaces Clave

### SynergyProvider
Ubicación: `com.mrsasayo.legacycreaturescorey.feature.synergy`

```java
public interface SynergyProvider {
    SynergyModule module();
    boolean validate();
    void onRegister();
    void onMobTiered(Mob mob, MobTier tier, MobLegacyData data);
    void onLootGenerated(Mob mob, MobTier tier, LootContext context, List<ItemStack> drops);
}
```
- **Ciclo de vida:** `validate()` se ejecuta durante el registro; `onRegister()` solo si la validación es exitosa.
- **Requisito:** devolver `false` en `validate()` cuando faltan tags o datos.
- **Default hooks:** todos los métodos (excepto `module()`) tienen implementaciones vacías por defecto, así que cada proveedor puede sobrescribir únicamente los que necesite.
- **Módulos soportados** (`SynergyModule`): ARMORY, ARCANEY, ARTIFACTYS, WORLDSCAPESYS, EVENTYS, SUMMONYS, PROGRESSIONYS, ENEMIESYS.

### Payloads de red
Ubicación: `com.mrsasayo.legacycreaturescorey.core.network`

| Payload | Dirección | Uso |
| --- | --- | --- |
| `ClientEffectPayload` | S2C | reproduce partículas/sonidos de efectos personalizados en el cliente |
| `DifficultySyncPayload` | S2C | snapshot de dificultad global/personal para el HUD |
| `ServerHelloPayload` | S2C | handshake del servidor al cliente |
| `ClientCapabilitiesPayload` | C2S | capacidades del cliente (feedback visual) |

Registro en 26.3 (ya no existe `playS2C`/`playC2S`):
```java
PayloadTypeRegistry.clientboundPlay().register(ClientEffectPayload.ID, ClientEffectPayload.CODEC);
PayloadTypeRegistry.serverboundPlay().register(ClientCapabilitiesPayload.ID, ClientCapabilitiesPayload.CODEC);
```
- `ClientEffectPayload.ID` es un `CustomPacketPayload.Type<T>` (26.3 renombró `CustomPayload.Id`).
- Los clientes registran receptores con `ClientPlayNetworking.registerGlobalReceiver(...)` y aplican el efecto según el `ClientEffectType` del payload.

### Data Attachments
Ubicación: `com.mrsasayo.legacycreaturescorey.core.component`

- `ModDataAttachments.PLAYER_DIFFICULTY`: adjunta `PlayerDifficultyData` a `ServerPlayer`.
- `ModDataAttachments.MOB_LEGACY`: adjunta `MobLegacyData` a cualquier `Mob`.
- Uso recomendado: `player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY)`.

## 🔁 Eventos Disponibles
Todos en `com.mrsasayo.legacycreaturescorey.core.api.event`:

| Evento | Descripción |
| --- | --- |
| `TierEvents.TIER_APPLIED` | se dispara cada vez que un mob obtiene un tier (forzado o natural) |
| `TierLootEvents.BEFORE_TIER_LOOT` / `AFTER_TIER_LOOT` | permiten vetar o complementar el botín adicional |
| `TieredLootTableEvents.MODIFY` / `POST_APPLY` | ajustan o auditan las tablas cargadas desde datapacks |
| `TierLootTelemetryEvents.TIER_LOOT_APPLIED` | snapshots antes/después de modificar drops (dashboards/alertas) |
| `AntiFarmEvents.SHOULD_IGNORE` / `THRESHOLD_MODIFIER` / `CHUNK_BLOCKED` | controlan la detección de granjas |
| `AntiFarmDashboardEvents.CHUNK_ACTIVITY_UPDATED` | feed continuo para paneles (razón, threshold, radio afectado) |
| `MutationDataEvents.MODIFY` / `POST_APPLY` | hooks para filtrar/complementar las mutaciones asignadas por datapack |

Hooks automáticos usados por el runtime: `ServerTickEvents.END_LEVEL_TICK` (26.3 renombró `END_WORLD_TICK`) y callbacks de daño de Fabric.

Otros puntos de extensión:
- `AntiFarmManager.shouldBlockTieredSpawns(mob)` puede emplearse en otros mods para respetar zonas calientes.
- `TierManager.forceTier(mob, tier, assignDefaults)` sigue disponible para promociones manuales.

## 🧩 Ejemplo de Integración Externa
```java
public class MySynergy implements SynergyProvider {
    @Override
    public SynergyModule module() {
        return SynergyModule.EVENTYS; // requiere que el mod esté cargado
    }

    @Override
    public boolean validate() {
        // Revisar tags o configuraciones personalizadas
        return FabricLoader.getInstance().isModLoaded("legacycreatureseventys");
    }

    @Override
    public void onMobTiered(Mob mob, MobTier tier, MobLegacyData data) {
        if (tier.isAtLeast(MobTier.MYTHIC)) {
            mob.addTag("eventys:boss_target"); // 26.3: addTag (antes addCommandTag)
        }
    }

    @Override
    public void onLootGenerated(Mob mob, MobTier tier, LootContext ctx, List<ItemStack> drops) {
        if (tier == MobTier.DEFINITIVE) {
            drops.add(new ItemStack(MyItems.EVENTYS_SIGIL));
        }
    }
}
```

Registrar el proveedor:
```java
if (FabricLoader.getInstance().isModLoaded("legacycreaturescorey")) {
    CoreyAPI.registerSynergyProvider(new MySynergy());
}
```

## 📄 Versionado
- API introducida en `CoreyAPI` v1 (MC 1.21.10) y re-verificada para **MC 26.3** (commit `ed3ae28`).
- Consumidores deben verificar cambios en `docs/API_REFERENCE.md` antes de actualizar.
- Cambios de nombres por la generación 26.x (MobEntity→Mob, ServerWorld→ServerLevel, etc.) están
  detallados en `docs/PORTING_NOTES.md` §3-§4.
