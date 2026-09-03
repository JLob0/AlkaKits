package com.alkacode.kits.gui;

import com.alkacode.kits.AlkaKitsPlugin;
import com.alkacode.kits.manager.KitManager;
import com.alkacode.kits.manager.KitProgressManager;
import com.alkacode.kits.model.Kit;
import com.alkacode.kits.model.KitGroup;
import com.alkacode.kits.model.KitProgress;
import com.alkacode.kits.model.KitStatus;
import com.alkacode.kits.service.KitClaimService;
import com.alkacode.kits.util.GuiStyle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Tela de uma categoria - dois modos, escolhidos pelo campo {@code group}:
 * <ul>
 *   <li>{@code group == null}: landing da categoria - mostra os kits SEM grupo
 *   (direto, ex: "booster_xp" em exclusivos) + um icone por grupo distinto (ex: "Campones"),
 *   clicar num grupo abre este mesmo menu de novo com {@code group} preenchido.</li>
 *   <li>{@code group != null}: mostra so os kits daquele grupo (ex: os 3 kits do
 *   rank Campones) - "voltar" retorna pra o modo landing da mesma categoria.</li>
 * </ul>
 * Clicar num KIT (em qualquer um dos dois modos) sempre abre {@link PreviewMenu}.
 *
 * <p>Layout: grade de ate 16 icones por pagina (4 linhas internas x ATE 5 colunas
 * por linha), SEMPRE centralizada verticalmente E horizontalmente conforme a
 * quantidade real de itens daquela pagina (ver {@link #slotsFor(int)}) - uma tela
 * com so 2-3 itens fica com eles no meio, nao amontoados num canto. Com exatamente
 * 10 itens (ex: os 10 grupos de rank em "kits gratis") o resultado e um bloco 2x5
 * limpo (linhas 2/3, colunas 2-6 = slots 20-24 e 29-33) em vez de 3 linhas
 * desiguais - decisao 2026-09-02 apos pedido do usuario pra esse layout especifico.
 * O campo {@code slot:} de cada Kit/KitGroup no YAML deixou de ser uma posicao
 * exata na tela pra virar so a ORDEM de exibicao (numero menor aparece primeiro) -
 * a posicao de verdade e sempre calculada aqui. Passa de 16 itens numa pagina e
 * aparecem os botoes de pagina anterior/proxima automaticamente.</p>
 */
public final class KitsMenu extends KitGui {

    private static final int PAGE_SIZE = 16;
    /** Linhas internas disponiveis (0=header, 5=voltar/paginacao, nunca usadas pra itens). */
    private static final int[][] CENTERED_ROWS = {
            {}, {2}, {2, 3}, {1, 2, 3}, {1, 2, 3, 4}
    };
    /** Colunas (dentro de uma linha de 9) centralizadas conforme quantos itens essa linha tem.
     * 5 itens usa colunas CONSECUTIVAS (2-6, sem gaps) em vez do padrao espacado de
     * 1-4 itens - decisao deliberada pra ficar compacto/em bloco (ver classe acima). */
    private static final int[][] CENTERED_COLS = {
            {}, {4}, {3, 5}, {2, 4, 6}, {1, 3, 5, 7}, {2, 3, 4, 5, 6}
    };
    private static final int PREV_SLOT = 46;
    private static final int NEXT_SLOT = 52;

    private final String category;
    private final String group;
    private final int page;
    private final KitManager kitManager;
    private final KitProgressManager progressManager;
    private final KitClaimService claimService;
    private final Consumer<Player> onBack;
    private final BiConsumer<Player, Kit> openPreview;
    private final BiConsumer<Player, String> openGroup;
    private final BiConsumer<Player, Integer> openPage;

    public KitsMenu(AlkaKitsPlugin plugin, Player player, String category, String group, int page,
                     KitManager kitManager, KitProgressManager progressManager, KitClaimService claimService,
                     Consumer<Player> onBack, BiConsumer<Player, Kit> openPreview,
                     BiConsumer<Player, String> openGroup, BiConsumer<Player, Integer> openPage) {
        super(plugin, player, "alkakits-kits");
        this.category = category;
        this.group = group;
        this.page = Math.max(0, page);
        this.kitManager = kitManager;
        this.progressManager = progressManager;
        this.claimService = claimService;
        this.onBack = onBack;
        this.openPreview = openPreview;
        this.openGroup = openGroup;
        this.openPage = openPage;
    }

    @Override
    public void render() {
        setAt('H', buildHeaderItem());
        setAt('V', commonVoltar(), e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            onBack.accept(player);
        });

        List<Entry> entries = collectEntries();
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.min(page, totalPages - 1);
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        int count = end - start;

        int[] slots = slotsFor(count);
        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(start + i);
            setItem(slots[i], entry.icon(), entry.action());
        }

        if (safePage > 0) {
            setItem(PREV_SLOT, icon("pagina-anterior"), e -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                openPage.accept(player, safePage - 1);
            });
        }
        if (safePage < totalPages - 1) {
            setItem(NEXT_SLOT, icon("proxima-pagina"), e -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                openPage.accept(player, safePage + 1);
            });
        }

        fillRest();
    }

    /** Slots (0-53) pra exibir exatamente {@code count} itens (1-16) centralizados -
     * ver CENTERED_ROWS/CENTERED_COLS. Distribui os itens em quantas linhas forem
     * necessarias (max 5/linha), o mais equilibrado possivel, e centraliza tanto o
     * bloco de linhas usadas quanto os itens dentro de cada linha parcial. */
    private static int[] slotsFor(int count) {
        count = Math.max(0, Math.min(count, PAGE_SIZE));
        if (count == 0) {
            return new int[0];
        }
        int rowsNeeded = (count + 4) / 5;
        int[] rows = CENTERED_ROWS[rowsNeeded];

        int base = count / rowsNeeded;
        int extra = count % rowsNeeded;

        int[] result = new int[count];
        int idx = 0;
        for (int r = 0; r < rowsNeeded; r++) {
            int itemsThisRow = base + (r < extra ? 1 : 0);
            int[] cols = CENTERED_COLS[itemsThisRow];
            int rowBase = rows[r] * 9;
            for (int col : cols) {
                result[idx++] = rowBase + col;
            }
        }
        return result;
    }

    private List<Entry> collectEntries() {
        List<Entry> entries = new ArrayList<>();
        if (group == null) {
            for (Kit kit : kitManager.getUngroupedKitsInCategory(category)) {
                entries.add(new Entry(buildKitIcon(kit), e -> openPreview.accept(player, kit)));
            }
            for (KitGroup kitGroup : kitManager.getGroupsInCategory(category)) {
                entries.add(new Entry(kitGroup.icon(), e -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    openGroup.accept(player, kitGroup.id());
                }));
            }
        } else {
            for (Kit kit : kitManager.getKitsInGroup(category, group)) {
                entries.add(new Entry(buildKitIcon(kit), e -> openPreview.accept(player, kit)));
            }
        }
        return entries;
    }

    private ItemStack buildHeaderItem() {
        ItemStack item;
        if (group != null) {
            KitGroup groupInfo = kitManager.getGroups().get(group);
            item = groupInfo != null ? groupInfo.icon().clone() : new ItemStack(Material.CHEST);
        } else {
            com.alkacode.kits.model.KitCategory categoryInfo = kitManager.getCategories().get(category);
            item = categoryInfo != null ? categoryInfo.icon().clone() : new ItemStack(Material.CHEST);
        }
        ItemMeta meta = item.getItemMeta();
        meta.lore(menu().lore(id + ".header", null));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildKitIcon(Kit kit) {
        ItemStack item = kit.getIcon().clone();
        ItemMeta meta = item.getItemMeta();

        KitProgress progress = progressManager.getProgress(player.getUniqueId(), kit.getId());
        int maxLevel = kit.getMaxLevel();

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>(meta.hasLore() && meta.lore() != null ? meta.lore() : List.of());
        lore.addAll(menu().lore(id + ".kit-nivel-lore",
                Map.of("atual", String.valueOf(progress.unlockedLevel()), "maximo", String.valueOf(maxLevel))));
        meta.lore(lore);

        boolean actionable = claimService.evaluateClaimStatus(player, kit) == KitStatus.CLAIMABLE
                || claimService.evaluateBuyStatus(player, kit) == KitStatus.PURCHASABLE;
        if (actionable) {
            GuiStyle.glow(meta);
        }

        item.setItemMeta(meta);
        return item;
    }

    private record Entry(ItemStack icon, Consumer<InventoryClickEvent> action) {
    }
}
