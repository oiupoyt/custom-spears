package dev.kat.customspears.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    // key format: uuid:ability (e.g. "uuid:frostbite" or "uuid:curse:POISON")
    private final Map<String, Long> cooldowns = new HashMap<>();

    public void set(UUID player, String ability, int seconds) {
        cooldowns.put(player + ":" + ability, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean isOnCooldown(UUID player, String ability) {
        Long expiry = cooldowns.get(player + ":" + ability);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(player + ":" + ability);
            return false;
        }
        return true;
    }

    /** Returns remaining cooldown in seconds (0 if not on cooldown) */
    public long getRemaining(UUID player, String ability) {
        Long expiry = cooldowns.get(player + ":" + ability);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return remaining > 0 ? (long) Math.ceil(remaining / 1000.0) : 0;
    }

    public void clear(UUID player) {
        cooldowns.entrySet().removeIf(e -> e.getKey().startsWith(player.toString()));
    }

    /** Returns all abilities currently on cooldown for a player with their remaining seconds */
    public Map<String, Long> getAllCooldowns(UUID player) {
        Map<String, Long> result = new HashMap<>();
        String prefix = player + ":";
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue() > now) {
                String ability = entry.getKey().substring(prefix.length());
                long remaining = (long) Math.ceil((entry.getValue() - now) / 1000.0);
                result.put(ability, remaining);
            }
        }
        return result;
    }
}
