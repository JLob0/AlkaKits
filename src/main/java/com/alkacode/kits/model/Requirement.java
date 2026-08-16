package com.alkacode.kits.model;

/**
 * Condicao pra desbloquear um kit/nivel. PERMISSION e WORLD sao checados sem
 * dependencia nenhuma; PLACEHOLDER precisa do PlaceholderAPI instalado (se nao
 * estiver, requisitos desse tipo falham fechado - ver RequirementService).
 */
public record Requirement(Type type, String value, Operator operator, String comparisonValue) {

    public enum Type {
        PERMISSION, WORLD, PLACEHOLDER
    }

    public enum Operator {
        EQUALS, NOT_EQUALS, GREATER_EQUALS, LESS_EQUALS, GREATER, LESS
    }

    public static Requirement permission(String node) {
        return new Requirement(Type.PERMISSION, node, null, null);
    }

    public static Requirement world(String worldName) {
        return new Requirement(Type.WORLD, worldName, null, null);
    }

    public static Requirement placeholder(String placeholder, Operator operator, String comparisonValue) {
        return new Requirement(Type.PLACEHOLDER, placeholder, operator, comparisonValue);
    }
}
