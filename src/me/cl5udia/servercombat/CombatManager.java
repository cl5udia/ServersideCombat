package me.cl5udia.servercombat;

import me.cl5udia.servercombat.util.AABB;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;
import java.util.stream.Collectors;

public class CombatManager implements Listener {
    private final float maxReach;
    private final boolean throughBlocks;
    private final Main plugin;

    public CombatManager(float maxReach, boolean throughBlocks, Main plugin) {
        this.maxReach = maxReach;
        this.throughBlocks = throughBlocks;
        this.plugin = plugin;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            if (!e.getDamager().hasMetadata("simulate_damage")) {
                e.setCancelled(true);
            } else {
                e.getDamager().removeMetadata("simulate_damage", plugin);
            }
        }
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent e) {
        Player p = e.getPlayer();
        PlayerAnimationType anim = e.getAnimationType();
        if (anim != PlayerAnimationType.ARM_SWING) return;
        handleClientInput(p);
    }

    private void handleClientInput(Player aggressor) {
        List<Entity> nearbyEntities = aggressor.getNearbyEntities(maxReach,
                maxReach, maxReach);
        List<Player> nearbyPlayers = nearbyEntities.stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());
        Player closestTarget = null;
        Float lowestReach = null;
        for (Player potentialTarget : nearbyPlayers) {
            AABB.Vec3D potentialIntersectingRay = findIntersectingRay(aggressor, potentialTarget, maxReach);
            if (potentialIntersectingRay != null) {
                float aggressorReach = (float) potentialIntersectingRay.magnitude();
                if (lowestReach == null || aggressorReach < lowestReach) {
                    lowestReach = aggressorReach;
                    closestTarget = potentialTarget;
                }
            }
        }
        if (closestTarget != null) {
            //Check if player is hitting through block
            if (!aggressor.hasLineOfSight(closestTarget) && !throughBlocks) return;
            aggressor.setMetadata("simulate_damage", new FixedMetadataValue(plugin, true));
            ((CraftPlayer) aggressor).getHandle().attack(((CraftPlayer) closestTarget).getHandle());
        }
    }

    private AABB.Vec3D findIntersectingRay(Player aggressor, Player target, float reach) {
        Location targetPos = target.getLocation();
        Location eyeLocation = aggressor.getEyeLocation();
        AABB.Vec3D origin = new AABB.Vec3D(eyeLocation.getX() - targetPos.getX(), eyeLocation.getY() - targetPos.getY(), eyeLocation.getZ() - targetPos.getZ());
        AABB.Vec3D direction = AABB.Vec3D.fromVector(eyeLocation.getDirection());

        AABB.Vec3D min = new AABB.Vec3D(-0.4, 0 + 0.01  , -0.4);
        AABB.Vec3D max = new AABB.Vec3D(0.4, (target.isSneaking() ? 1.6 : 1.9), 0.4);
        AABB box = new AABB(min, max);

        AABB.Ray3D ray = new AABB.Ray3D(origin, direction);
        AABB.Vec3D intersectionPoint = box.intersectsRay(ray, 0, reach);
        return intersectionPoint == null? null : intersectionPoint.subtract(origin);
    }
}
