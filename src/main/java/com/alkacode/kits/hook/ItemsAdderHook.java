package com.alkacode.kits.hook;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * Hook do ItemsAdder via reflection - ItemsAdder e softdepend, entao NAO dependemos
 * do jar em compile-time. Resolve um id "namespace:id" (ex: "boxpixstudio:check_v1")
 * pro ItemStack do custom item. Se o IA nao estiver presente ou o id nao existir,
 * retorna null e o chamador cai no material vanilla de fallback.
 *
 * <p>Mesmo padrao do AlkaCrates.hook.item.ItemsAdderHook - se a API do IA mudar, so
 * este arquivo precisa de ajuste.
 */
public final class ItemsAdderHook {

    private static Method getInstance;
    private static boolean initialized = false;
    private static boolean available = false;

    private ItemsAdderHook() {
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            getInstance = customStackClass.getMethod("getInstance", String.class);
            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    /** True se o ItemsAdder esta carregado e a API respondeu. */
    public static boolean isAvailable() {
        init();
        return available;
    }

    /** Um id de IA e sempre "namespace:id". So tratamos como IA se tiver ':'. */
    public static boolean looksLikeIaId(String id) {
        return id != null && id.indexOf(':') > 0;
    }

    /**
     * Resolve "namespace:id" pro ItemStack do custom item, ou null se IA ausente/id
     * inexistente. Retorna um clone proprio (seguro pra o chamador aplicar meta).
     */
    public static ItemStack resolve(String id) {
        init();
        if (!available || !looksLikeIaId(id)) {
            return null;
        }
        try {
            Object stack = getInstance.invoke(null, id);
            if (stack == null) {
                return null;
            }
            Method getItemStack = stack.getClass().getMethod("getItemStack");
            Object item = getItemStack.invoke(stack);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
