package org.opencommunity.regen;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.inventory.ItemStack;
import org.opencommunity.regen.events.BlockRegenEvent;
import org.opencommunity.regen.events.EntityRegenEvent;

import java.util.List;


/**
 * Detects all explosions and process for
 * regeneration of destroyed entities and
 * Blocks.
 *
 * @author ElgarL
 */
public class Detection implements Listener {

    private final Regen plugin;
    private final FileHandler handler;

    /**
     * Constructor for our Event Listener.
     *
     * @param plugin Regen instance.
     */
    public Detection(Regen plugin) {

        this.plugin = plugin;
        this.handler = this.plugin.getFileHandler();
    }

    /*
     * Track ITEM_FRAME and PAINTING
     * destroyed in explosions.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakEvent event) {

        if (event.getCause() != RemoveCause.EXPLOSION) return;

        Entity entity = event.getEntity();

        if (entity.getType() != EntityType.ITEM_FRAME
                && entity.getType() != EntityType.PAINTING) return;

        EntityRegenEvent customEvent = new EntityRegenEvent(entity);
        plugin.getServer().getPluginManager().callEvent(customEvent);

        if (customEvent.isCancelled()) return;

        parseEntity(event.getEntity());
    }

    /*
     * Track ARMOR_STAND
     * destroyed in explosions.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        if (event.getCause() != DamageCause.ENTITY_EXPLOSION) return;

        Entity entity = event.getEntity();

        if (entity.getType() != EntityType.ARMOR_STAND) return;

        EntityRegenEvent customEvent = new EntityRegenEvent(entity);
        plugin.getServer().getPluginManager().callEvent(customEvent);

        if (customEvent.isCancelled()) return;

        parseEntity(event.getEntity());
    }

    /*
     * All blocks destroyed in explosions.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {

        BlockRegenEvent customEvent = new BlockRegenEvent(event.blockList());
        plugin.getServer().getPluginManager().callEvent(customEvent);

        if (customEvent.isCancelled()) return;

        event.blockList().removeIf(block -> (!customEvent.blockList().contains(block)));

        parseBlocks(event.blockList());
        event.setYield(0);
    }

    /*
     * All blocks destroyed in explosions.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {

        BlockRegenEvent customEvent = new BlockRegenEvent(event.blockList());
        plugin.getServer().getPluginManager().callEvent(customEvent);

        if (customEvent.isCancelled()) return;

        event.blockList().removeIf(block -> (!customEvent.blockList().contains(block)));

        parseBlocks(event.blockList());
        event.setYield(0);
    }

    /**
     * Process the following Entities for regeneration.
     * ITEM_FRAME, PAINTING,ARMOR_STAND.
     *
     * @param <T>    Entity Type.
     * @param entity an Entity to parse.
     */
    public <T extends Entity> void parseEntity(T entity) {

        /*
         * Remove the entity so it
         * doesn't drop an ItemStack.
         */
        switch (entity.getType()) {

            case ITEM_FRAME:

                this.handler.saveEntity(entity);
                ((ItemFrame) entity).setItem(null);
                entity.remove();
                break;

            case PAINTING:

                this.handler.saveEntity(entity);
                entity.remove();
                break;

            case ARMOR_STAND:

                ArmorStand stand = (ArmorStand) entity;

                if (!stand.isInvulnerable()) {

                    this.handler.saveEntity(stand);
                    stand.getEquipment().clear();
                }
                break;

            default:

                //System.out.println("*** Entity type: " + entity.getType().name());
        }
    }

    /**
     * Process all Blocks for regeneration.
     *
     * @param blockList a LIst of Blocks to parse.
     */
    public void parseBlocks(List<Block> blockList) {

        this.handler.saveBlocks(blockList);

        for (Block block : blockList) {
            BlockState state = block.getState();

            /*
             * Prevent item drops from containers
             * and from blocks that are not
             * destroyed by explosions.
             */
            boolean setAir = false;

            switch (state.getType()) {

                case LECTERN:

                    Lectern lectern = (Lectern) state;
                    lectern.getInventory().clear();
                    setAir = true;
                    break;

                case BEACON:

                    setAir = true;
                    break;

                case JUKEBOX:

                    // TODO does nothing, still drops a record.
                    ((Jukebox) state).stopPlaying();
                    ((Jukebox) state).setRecord(new ItemStack(Material.AIR));
                    setAir = true;
                    break;

                default:

                    if (state instanceof Container container) {

                        container.getInventory().clear();
                        setAir = true;
                    }

                    if (Tag.DOORS.isTagged(state.getType())) setAir = true;
            }

            if (setAir) {

                state.setType(Material.AIR);
                state.update(true, false);
            }
        }
    }
}
