package gg.crystalized.lobby;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public enum Cosmetics {
    // ATTENTION!!!!
    // The order of the cosmetics here MUST NOT BE CHANGED.
    // otherwise it will mess up the database
    HEADPHONES("crystalized:cosmetic/head/headphones", 1, null, EquipmentSlot.HEAD);
    final String itemModel;
    final Integer obtainableLevel;
    final Integer price;
    final EquipmentSlot slot;
    Cosmetics(String itemModel, Integer obtainableLevel, Integer price, EquipmentSlot slot){
        this.itemModel = itemModel;
        this.obtainableLevel = obtainableLevel;
        this.price = price;
        this.slot = slot;
    }
}
