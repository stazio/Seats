package io.staz.sponge.seats;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableFoodData;
import org.spongepowered.api.data.manipulator.immutable.entity.ImmutableHealthData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;

import java.util.HashMap;
import java.util.UUID;

@Plugin(id = "seats")
public class SeatsPlugin {
    private HashMap<UUID, Chair> playerChairs = new HashMap<>();

    @Listener
    public void preInit(GamePreInitializationEvent event) {
        Sponge.getCommandManager().register(this,
                CommandSpec.builder().executor((src, args) -> {
                    Player player = (Player) args.getOne(Text.of("player")).orElse(src);
                    if (playerChairs.containsKey(player.getUniqueId())) {
                        unseatPlayer(player.getUniqueId());
                    }else {
                        Location location = player.getLocation();
                        Entity entity = location.getExtent().createEntity(EntityTypes.ARMOR_STAND, location.add(0, -1.65, 0).getPosition());
                        entity.offer(Keys.INVISIBLE, true);
                        entity.offer(Keys.HAS_GRAVITY, false);
                        location.getExtent().spawnEntity(entity);
                        entity.addPassenger(player);
                        Chair chair = new Chair();
                        chair.entity = entity;
                        chair.force = args.hasAny("force");
                        chair.playerUUID = player.getUniqueId();
                        chair.health = player.getHealthData().asImmutable();
                        chair.nutrition = player.getFoodData().asImmutable();
                        playerChairs.put(player.getUniqueId(), chair);
                    }
                    return CommandResult.builder().affectedEntities(2).affectedBlocks(1).build();
                }).permission("seats.sit").
                        description(Text.of("Allows a player to sit wherever they are standing.")).
                        arguments(GenericArguments.flags().
                                flag("force").
                                setAcceptsArbitraryLongFlags(true).setAnchorFlags(false)      .                         permissionFlag("seats.sit.force").buildWith(GenericArguments.
                                optional(GenericArguments.
                                        requiringPermission(GenericArguments.
                                                player(Text.of("player")),
                                                "seats.sit.others")))
                        )
                        .build(), "sit");
        Sponge.getScheduler().createTaskBuilder().intervalTicks(10).execute(() -> playerChairs.forEach((playerUUID, chair) -> {
            Player player = Sponge.getServer().getPlayer(playerUUID).orElse(null);
            if (player != null && player.isOnline()) {
                if (chair.entity.getPassengers().size() <= 0) {
                    if (chair.force)
                        chair.entity.addPassenger(player);
                    else {
                        unseatPlayer(playerUUID);
                    }
                }
            } else {
                unseatPlayer(playerUUID);
            }
        })).name("Char-Checker").submit(this);
    }

    private void unseatPlayer(UUID player) {
        playerChairs.get(player).entity.remove();
        playerChairs.remove(player);
    }

    private static class Chair {
        public Entity entity;
        public UUID playerUUID;
        public ImmutableHealthData health;
        public ImmutableFoodData nutrition;
        public boolean force;
    }
}
