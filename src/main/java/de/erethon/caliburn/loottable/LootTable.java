/*
 * Copyright (C) 2015-2019 Daniel Saukel.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.erethon.caliburn.loottable;

import de.erethon.caliburn.CaliburnAPI;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.compatibility.Version;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * A loot table e.g. for mob drops.
 *
 * @author Daniel Saukel
 */
public class LootTable implements ConfigurationSerializable {

    public static final String MAIN_HAND = "mainHand";
    public static final String OFF_HAND = "offHand";
    public static final String HELMET = "helmet";
    public static final String CHESTPLATE = "chestplate";
    public static final String LEGGINGS = "leggings";
    public static final String BOOTS = "boots";

    public class Entry {

        private String id;
        private ItemStack item;
        private double chance;

        public Entry(String id, ItemStack item, double chance) {
            this.id = id;
            this.item = item;
            setLootChance(chance);
        }

        public Entry(Map<String, Object> args) {
            item = CaliburnAPI.getInstance().deserializeStack(args.get("item"));
            Object chance = args.get("chance");
            if (chance instanceof Number) {
                setLootChance(((Number) chance).doubleValue());
            }
        }

        /* Getters and setters */
        /**
         * @return the id of the loot table entry
         */
        public String getId() {
            return id;
        }

        /**
         * @param id the id of the loot table entry to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return the loot item stack
         */
        public ItemStack getLootItem() {
            return item;
        }

        /**
         * @param item the loot item to set
         */
        public void setLootItem(ItemStack item) {
            this.item = item;
        }

        /**
         * @return the loot chance
         */
        public double getLootChance() {
            return chance;
        }

        /**
         * @param chance the loot chance to set
         */
        public void setLootChance(double chance) {
            if (chance < 0d) {
                chance = 0d;
            } else if (chance > 100d) {
                chance = 100d;
            }
            this.chance = chance;

        }

        public Map<String, Object> serialize() {
            Map<String, Object> config = new HashMap<>();
            config.put("item", item);
            config.put("chance", chance);
            return config;
        }

    }

    private String name;
    private Map<String, Entry> entries = new HashMap<>();

    /**
     * @param file the script file
     */
    public LootTable(CaliburnAPI api, File file) {
        this(api, file.getName().substring(0, file.getName().length() - 4), YamlConfiguration.loadConfiguration(file));
    }

    /**
     * @param name   the name of the loot table
     * @param config the config that stores the information
     */
    public LootTable(CaliburnAPI api, String name, FileConfiguration config) {
        api.getLootTables().add(this);

        this.name = name;

        for (String id : config.getKeys(false)) {
            ItemStack item = api.deserializeStack(config, id + ".item");
            if (item == null) {
                continue;
            }

            double chance = config.getDouble(id + ".chance");
            entries.put(id, new Entry(id, item, chance));
        }
    }

    public LootTable(Map<String, Object> args) {
        for (Map.Entry<String, Object> mapEntry : args.entrySet()) {
            try {
                Entry entry = new Entry((Map<String, Object>) mapEntry.getValue());
                entry.setId(mapEntry.getKey());
                entries.put(mapEntry.getKey(), entry);
            } catch (ClassCastException exception) {
                MessageUtil.log(ChatColor.RED + "Skipping erroneous loot table entry \"" + mapEntry.getKey() + "\".");
            }
        }
    }

    /**
     * @param api  the API instance
     * @param name the name of the loot table
     */
    public LootTable(CaliburnAPI api, String name) {
        api.getLootTables().add(this);
        this.name = name;
    }

    /* Getters and setters */
    /**
     * @return the name of the loot table
     */
    public String getName() {
        return name;
    }

    /**
     * @return the entries
     */
    public Collection<Entry> getEntries() {
        return entries.values();
    }

    /**
     * Returns the entry with the given ID.
     *
     * @param id the entry ID
     * @return the entry with the given ID.
     */
    public Entry getEntry(String id) {
        return entries.get(id);
    }

    /**
     * @param entry the entry to add
     */
    public void addEntry(Entry entry) {
        entries.put(entry.getId(), entry);
    }

    /**
     * @param entry the entry to remove
     */
    public void removeEntry(Entry entry) {
        entries.remove(entry.getId());
    }

    /**
     * Overrides the values of the given instance of EntityEquipment.<p>
     * Values are taken from the entries with the IDs specified in the constants in this class.<p>
     * These are: "mainHand", "offHand", "helmet", "chestplate", "leggings" and "boots".
     *
     * @param entityEquip the instance of EntityEquipment to override
     */
    public void setEntityEquipment(EntityEquipment entityEquip) {
        boolean off = Version.isAtLeast(Version.MC1_9);
        Entry mainHand = getEntry(LootTable.MAIN_HAND);
        Entry offHand = getEntry(LootTable.OFF_HAND);
        if (off) {
            entityEquip.setItemInMainHand(mainHand.getLootItem());
            entityEquip.setItemInMainHandDropChance((float) (mainHand.getLootChance() / 100d));
            entityEquip.setItemInOffHand(offHand.getLootItem());
            entityEquip.setItemInOffHandDropChance((float) (mainHand.getLootChance() / 100d));
        } else {
            entityEquip.setItemInHand(mainHand.getLootItem());
            entityEquip.setItemInHandDropChance((float) (mainHand.getLootChance() / 100d));
        }

        Entry helmet = getEntry(LootTable.HELMET);
        if (helmet != null) {
            entityEquip.setHelmet(helmet.getLootItem());
            entityEquip.setHelmetDropChance((float) (helmet.getLootChance() / 100d));
        }

        Entry chestplate = getEntry(LootTable.CHESTPLATE);
        if (chestplate != null) {
            entityEquip.setHelmet(chestplate.getLootItem());
            entityEquip.setHelmetDropChance((float) (chestplate.getLootChance() / 100d));
        }

        Entry leggings = getEntry(LootTable.LEGGINGS);
        if (leggings != null) {
            entityEquip.setHelmet(leggings.getLootItem());
            entityEquip.setHelmetDropChance((float) (leggings.getLootChance() / 100d));
        }

        Entry boots = getEntry(LootTable.BOOTS);
        if (boots != null) {
            entityEquip.setHelmet(boots.getLootItem());
            entityEquip.setHelmetDropChance((float) (boots.getLootChance() / 100d));
        }
    }

    /* Actions */
    /**
     * Adds loot to a list randomly based on the chance value
     *
     * @return a list of the loot
     */
    public List<ItemStack> generateLootList() {
        List<ItemStack> lootList = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (new Random().nextInt(100) < entry.getLootChance()) {
                lootList.add(entry.getLootItem());
            }
        }
        return lootList;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> config = new HashMap<>();
        entries.values().forEach(e -> config.put(e.getId(), e.serialize()));
        return config;
    }

    @Override
    public String toString() {
        return "LootTable{Name=" + name + "}";
    }

}
