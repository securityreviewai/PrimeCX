package com.primecx.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.primecx.model.TicketCategory;

/**
 * Normalizes and validates support skill tokens against allowed {@link TicketCategory} names.
 */
public final class RoutingSkillNormalizer {

    private static final Set<String> ALLOWED = Arrays.stream(TicketCategory.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private RoutingSkillNormalizer() {}

    public static Set<String> normalize(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            String t = s.trim().toUpperCase().replace('-', '_');
            if (!ALLOWED.contains(t)) {
                throw new IllegalArgumentException("Invalid support skill: " + s.strip());
            }
            out.add(t);
            if (out.size() > 12) {
                throw new IllegalArgumentException("Too many support skills (max 12)");
            }
        }
        return out;
    }
}
