package gg.crystalized.lobby;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public class Cosmetic {
    public static ArrayList<Cosmetic> cosmetics = new ArrayList<>();
    final int id;
    final String itemModel;
    final Integer obtainableLevel;
    final Integer price;
    final EquipmentSlot slot;
    final Component name;

    Cosmetic(int id, String itemModel, Integer obtainableLevel, Integer price, EquipmentSlot slot, Component name) {
        this.id = id;
        this.itemModel = itemModel;
        this.obtainableLevel = obtainableLevel;
        this.price = price;
        this.slot = slot;
        this.name = name;
    }

    public static void createCosmetics(InputStream stream) {
        JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
        Map<String, JsonElement> map = json.asMap();
        for (String s : map.keySet()) {
            JsonObject j = map.get(s).getAsJsonObject();
            Cosmetic c = new Cosmetic(j.get("id").getAsInt(), j.get("model").getAsString(), getInt(j.get("level")), getInt(j.get("price")), getSlot(j.get("slot")), Component.translatable(j.get("name").getAsString()));
            cosmetics.add(c);
        }
    }

    public static Integer getInt(JsonElement j) {
        try {
            Integer i = j.getAsInt();
            return i;
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    public static EquipmentSlot getSlot(JsonElement json) {
        String s = json.getAsString();
        return switch (s) {
            case "HEAD" -> EquipmentSlot.HEAD;
            case "HAND" -> EquipmentSlot.HAND;
            case "OFF_HAND" -> EquipmentSlot.OFF_HAND;
            default -> null;
        };
    }

    public ItemStack build(Boolean wearing, Boolean open) {
        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey("crystalized", itemModel));
        if (wearing != null && wearing && slot == EquipmentSlot.HAND) {
            meta.displayName(Component.translatable("crystalized.item.shardcore3.name").color(LIGHT_PURPLE).decoration(BOLD, true).decoration(ITALIC, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.translatable("crystalized.item.shardcore3.desc").color(WHITE).decoration(ITALIC, false));
            meta.lore(lore);
            if (open) {
                meta.setCustomModelData(1);
            }
        } else {
            meta.displayName(name.color(WHITE).decoration(ITALIC, false));
            meta.lore(getDescription(wearing));
        }
        item.setItemMeta(meta);
        return item;
    }

    public ArrayList<Component> getDescription(Boolean wearing) {
        ArrayList<Component> desc = new ArrayList<>();
        if (wearing != null && wearing) {
            desc.add(Component.text("[Right-click] take off").color(WHITE).decoration(ITALIC, false));
        } else if (wearing != null && !wearing) {
            desc.add(Component.text("[Right-click] equip").color(WHITE).decoration(ITALIC, false));
        } else if (obtainableLevel != null) {
            desc.add(Component.text("[Right-click] unlock at level " + obtainableLevel).color(WHITE).decoration(ITALIC, false));
        } else {
            desc.add(Component.text("[Right-click] price: " + price).color(WHITE).decoration(ITALIC, false));
        }
        desc.add(Component.text("[Left-click] view").color(WHITE).decoration(ITALIC, false));
        desc.add(Component.text(slot.toString()).color(BLUE).decoration(ITALIC, false));
        return desc;
    }

    //TODO add translatables to everything

    public static void placeCosmetics(Player p, App a, int page) {
        Inventory inv = Bukkit.getServer().createInventory(null, 54, Component.text("\uA000\uA00A").color(WHITE));
        App.UITemplates.createUI(inv, App.useCases.ShopPage);
        if (a == App.WebButton) {
            //TODO set website URL here
            return;
        }
        int i = (page - 1) * 15;
        for (Cosmetic c : cosmetics) {
            if (c.slot != a.extra || c.ownsCosmetic(p)) {
                continue;
            }

            if (InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0) != null) {
                inv.setItem(InventoryManager.placeOnRightSlot(i, 51, 3, 1, 0), c.build(null, false));
            } else {
                break;
            }
            i++;
        }
        p.openInventory(inv);
    }
    //TODO make shardcores equippable

    // 0 = false
    // 1 = true
    public static Cosmetic identifyCosmetic(ItemStack item) {
        for (Cosmetic c : cosmetics) {
            if (item.getItemMeta() == null) {
                continue;
            }
            if (Objects.equals(item.getItemMeta().getItemModel(), new NamespacedKey("crystalized", c.itemModel))) {
                return c;
            }
        }
        return null;
    }

    public boolean isWearing(OfflinePlayer p) {
        if (!ownsCosmetic(p)) {
            return false;
        }
        ArrayList<Object[]> list = LobbyDatabase.fetchCosmetics(p);
        boolean wear = false;
        for (Object[] o : list) {
            if ((Integer) o[1] != id) {
                continue;
            }
            if ((Integer) o[2] == 1) {
                wear = true;
                break;
            }
        }
        return wear;
    }

    public boolean ownsCosmetic(OfflinePlayer p) {
        ArrayList<Object[]> list = LobbyDatabase.fetchCosmetics(p);
        boolean own = false;
        for (Object[] o : list) {
            if ((Integer) o[1] == id) {
                own = true;
                break;
            }
        }
        return own;
    }

    public static Cosmetic getShardcore(Player p) {
        ArrayList<Object[]> list = LobbyDatabase.fetchCosmetics(p);
        for (Object[] o : list) {
            if (cosmetics.get((Integer) o[1]).slot == EquipmentSlot.HAND && ((Integer) o[2]) == 1) {
                return getCosmeticById((Integer) o[1]);
            }
        }
        return null;
    }

    public static Cosmetic getCosmeticById(int id) {
        for (Cosmetic c : cosmetics) {
            if (c.id == id) {
                return c;
            }
        }
        return null;
    }

    public void clicked(ClickType click, Player p, InventoryView view) {
        if (click.isRightClick()) {
            if(!ownsCosmetic(p)) {
                if (price == null) {
                    return;
                }

                if (LevelManager.getMoney(p) < price) {
                    p.sendMessage(Component.text("You can't afford this cosmetic :(").color(RED));
                    return;
                }

                LobbyDatabase.addCosmetic(p, this, false);
                LevelManager.giveMoney(p, price * (-1));
                App.Shop.action(p);
            }else {
                if (isWearing(p)) {
                    p.sendEquipmentChange(p, slot, null);
                } else {
                    if (slot != EquipmentSlot.HAND) {
                        p.sendEquipmentChange(p, slot, build(true, false));
                    } else {
                        p.sendEquipmentChange(p, slot, build(true, true));
                    }
                }
                LobbyDatabase.cosmeticSetWearing(p, this, !isWearing(p));
            }
        } else if (click.isLeftClick()) {
            //TODO add view feature
        }
    }
}