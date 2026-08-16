package com.alkacode.kits.manager;

import com.alkacode.core.scheduler.AlkaScheduler;
import com.alkacode.kits.database.KitsRepository;
import com.alkacode.kits.model.KitProgress;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache em memoria do progresso de kits por jogador online - carregado inteiro (todos
 * os kits de uma vez, {@link KitsRepository#loadAllProgress}) no join, nunca reconsultado
 * durante a sessao a nao ser por escrita propria (compra/claim atualizam o cache E o
 * banco juntos - write-through). Diferente do PlayerTimeManager do AlkaTime, nao ha
 * acumulo de sessao aqui pra "flush" no quit: cada compra/claim ja persiste na hora.
 */
public final class KitProgressManager {

    private final KitsRepository repository;
    private final AlkaScheduler scheduler;

    private final Map<UUID, Map<String, KitProgress>> cache = new ConcurrentHashMap<>();

    public KitProgressManager(KitsRepository repository, AlkaScheduler scheduler) {
        this.repository = repository;
        this.scheduler = scheduler;
    }

    public void onJoin(UUID uuid) {
        // putIfAbsent por kit (nunca um put/overwrite do mapa inteiro) - se um grant-trigger
        // de primeiro-join disparar antes desse load assincrono terminar, a entrada que ele
        // escreveu sobrevive em vez de ser sobrescrita pelo valor (mais antigo) vindo do banco.
        scheduler.runAsync(() -> {
            Map<String, KitProgress> loaded = repository.loadAllProgress(uuid);
            Map<String, KitProgress> playerCache = cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            loaded.forEach(playerCache::putIfAbsent);
        });
    }

    public void onQuit(UUID uuid) {
        cache.remove(uuid);
    }

    /** false enquanto o load assincrono do join ainda nao terminou - callers devem recusar a acao (nunca assumir progresso zerado) nesse meio tempo, ver KitClaimService. */
    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public KitProgress getProgress(UUID uuid, String kitId) {
        Map<String, KitProgress> playerCache = cache.get(uuid);
        if (playerCache == null) {
            return KitProgress.EMPTY;
        }
        return playerCache.getOrDefault(kitId, KitProgress.EMPTY);
    }

    /** Write-through: atualiza o cache na hora (leitura seguinte imediata ja reflete) e agenda a persistencia assincrona (R7). */
    public void saveProgress(UUID uuid, String kitId, KitProgress progress) {
        cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kitId, progress);
        scheduler.runAsync(() -> repository.saveProgress(uuid, kitId, progress));
    }
}
