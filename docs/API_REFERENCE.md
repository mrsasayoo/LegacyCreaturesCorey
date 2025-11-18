# API REFERENCE - Legacy Creatures Corey

## 📦 Paquete Público
`com.mrsasayo.legacycreaturescorey.api`

### CoreyAPI
Helper estático para que otros mods interactúen con el ecosistema Corey sin depender de clases internas.

#### Métodos

##### `Optional<MobEntity> spawnCategorizedMob(ServerWorld world, BlockPos pos, EntityType<? extends MobEntity> type, MobTier tier)`
- **Descripción:** genera un mob del tipo indicado, lo promueve al `tier` solicitado y aplica mutaciones por defecto.
- **Retorno:** `Optional` con la entidad creada (vacío si falló el spawn).
- **Errores:** devuelve `Optional.empty()` cuando `world/type/pos` son nulos o el spawn falla.

##### `Optional<MobEntity> spawnCategorizedMob(..., List<Identifier> forcedMutations)`
- **Descripción:** idéntico al anterior, pero permite forzar una lista de mutaciones exacta.
- **Notas:** si `forcedMutations` no está vacío se limpian las mutaciones que asignaría `TierManager`.

##### `Optional<MobTier> getTier(MobEntity mob)`
- Recupera el tier actual desde `MobLegacyData`. Vuelve vacío si la entidad no posee el componente.

##### `List<Identifier> getMutations(MobEntity mob)`
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
- `Optional<MobTier> getTier(MobEntity)`: obtiene el tier ya asignado (vacío si está en `NORMAL`).
- `void forceTier(MobEntity, MobTier, boolean assignDefaultMutations)`: atajo oficial al `TierManager`.

> Nota: `getTier` refleja exactamente el estado del componente `MobLegacyData`; regresar `Optional.empty()` significa que el mob sigue en `NORMAL` o todavía no fue categorizado.

### AntiFarmDashboardApi
Pensado para paneles que quieran mostrar calor anti-farm.

- `Map<Long, ChunkActivityData> getChunkActivitySnapshot(MinecraftServer)`: snapshot completo (chunk key → datos).
- `Optional<ChunkActivityData> getChunkActivity(MinecraftServer, ChunkPos)`: detalle puntual.
- `boolean isChunkBlocked(MinecraftServer, ChunkPos)`: comprueba si el chunk sigue vetado.

## 🔌 Interfaces Clave

### SynergyProvider
Ubicación: `com.mrsasayo.legacycreaturescorey.synergy`

```java
public interface SynergyProvider {
    SynergyModule module();
    boolean validate();
    void onRegister();
    void onMobTiered(MobEntity mob, MobTier tier, MobLegacyData data);
    void onLootGenerated(MobEntity mob, MobTier tier, LootContext context, List<ItemStack> drops);
}
```
- **Ciclo de vida:** `validate()` se ejecuta durante el registro; `onRegister()` solo si la validación es exitosa.
- **Requisito:** devolver `false` en `validate()` cuando faltan tags o datos.
- **Default hooks:** todos los métodos (excepto `module()`) tienen implementaciones vacías por defecto, así que cada proveedor puede sobrescribir únicamente los que necesite.

### ClientEffectPayload
Ubicación: `com.mrsasayo.legacycreaturescorey.network`
- `PayloadTypeRegistry.playS2C().register(ClientEffectPayload.ID, ClientEffectPayload.CODEC)`
- Los clientes deben registrar un receptor que reproduzca partículas/sonidos según el campo `effectId`.

### Data Attachments
- `ModDataAttachments.PLAYER_DIFFICULTY`: adjunta `PlayerDifficultyData` a `ServerPlayerEntity`.
- `ModDataAttachments.MOB_LEGACY`: adjunta `MobLegacyData` a cualquier `MobEntity`.
- Uso recomendado: `player.getAttachedOrCreate(ModDataAttachments.PLAYER_DIFFICULTY)`.

## 🔁 Eventos Disponibles
- `TierEvents.TIER_APPLIED`: se dispara cada vez que un mob obtiene un tier (forzado o natural).
- `TierLootEvents.BEFORE_TIER_LOOT` / `AFTER_TIER_LOOT`: permiten vetar o complementar el botín adicional.
- `TieredLootTableEvents.MODIFY` / `POST_APPLY`: ajustan o auditan las tablas cargadas desde datapacks.
- `AntiFarmEvents.SHOULD_IGNORE` / `THRESHOLD_MODIFIER` / `CHUNK_BLOCKED`: controlan la detección de granjas.
- `AntiFarmDashboardEvents.CHUNK_ACTIVITY_UPDATED`: feed continuo para paneles (razón, threshold, radio afectado).
- `MutationRuntime` expone hooks automáticos (`ServerTickEvents.END_WORLD_TICK`, `ServerLivingEntityEvents.AFTER_DAMAGE`).
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
    public void onMobTiered(MobEntity mob, MobTier tier, MobLegacyData data) {
        if (tier.isAtLeast(MobTier.MYTHIC)) {
            mob.addCommandTag("eventys:boss_target");
        }
    }

    @Override
    public void onLootGenerated(MobEntity mob, MobTier tier, LootContext ctx, List<ItemStack> drops) {
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
- API introducida en `CoreyAPI` v1 (commit actual). Consumidores deben verificar cambios en `docs/API_REFERENCE.md` antes de actualizar.
