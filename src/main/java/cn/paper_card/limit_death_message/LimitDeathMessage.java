package cn.paper_card.limit_death_message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public final class LimitDeathMessage extends JavaPlugin implements Listener {

    private final @NotNull HashMap<UUID, Long> lastBroadcastTime;

    public LimitDeathMessage() {
        this.lastBroadcastTime = new HashMap<>();
    }


    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onDeath(@NotNull PlayerDeathEvent event) {
        final Player player = event.getPlayer();
        final Component component = event.deathMessage();

        if (component == null) return;

        final Long lastTime;
        synchronized (this.lastBroadcastTime) {
            lastTime = this.lastBroadcastTime.get(player.getUniqueId());
        }

        final long current = System.currentTimeMillis();

        // 节流
        if (lastTime != null && current < lastTime + 30 * 1000L) {
            final Location location = player.getLocation();
            final World world = location.getWorld();

            if (world == null) {
                player.sendMessage(Component.text()
                        .append(component)
                        .append(Component.text(" [自己]")
                                .color(NamedTextColor.GRAY)
                                .decorate(TextDecoration.ITALIC)
                        )
                        .build());
                event.deathMessage(null);
                return;
            }

            final int cx = location.getBlockX();
            final int cz = location.getBlockZ();

            // 播报给附近的玩家
            for (final Player worldPlayer : world.getPlayers()) {
                final Location loc = worldPlayer.getLocation();
                final int dx = Math.abs(loc.getBlockX() - cx);
                final int dz = Math.abs(loc.getBlockZ() - cz);
                if (dx < 1024 && dz < 1024) {
                    player.sendMessage(Component.text()
                            .append(component)
                            .append(Component.text(" [附近]")
                                    .color(NamedTextColor.GRAY)
                                    .decorate(TextDecoration.ITALIC))
                            .hoverEvent(HoverEvent.showText(Component.text("(dx:%d,dz:%d)".formatted(dx, dz))))
                            .build());
                }
            }

            event.deathMessage(null);
            return;
        }

        synchronized (this.lastBroadcastTime) {
            this.lastBroadcastTime.put(player.getUniqueId(), current);
        }
    }
}
