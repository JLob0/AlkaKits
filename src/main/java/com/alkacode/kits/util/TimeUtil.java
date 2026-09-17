package com.alkacode.kits.util;

public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * Formata segundos restantes como "1d 4h 32m", "45m 10s" ou "30s" (unidades zeradas
     * a esquerda sao omitidas; sempre mostra pelo menos os segundos). Mesmo formato do
     * TimeUtil do AlkaRankUp - convencao da rede, plugins nao compartilham essa classe.
     */
    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (days > 0 || hours > 0) {
            builder.append(hours).append("h ");
        }
        if (days > 0 || hours > 0 || minutes > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(seconds).append("s");
        return builder.toString();
    }
}
