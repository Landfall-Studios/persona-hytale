package world.landfall.statecraft.util;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class UtilCodecs {
    public static class PlayerInventory {
        public ItemContainer ARMOR;
        public ItemContainer BACKPACK;
        public ItemContainer TOOL;
        public ItemContainer HOTBAR;
        public ItemContainer UTILITY;
        public ItemContainer STORAGE;

        public static final BuilderCodec<PlayerInventory> CODEC = BuilderCodec.<PlayerInventory>builder(PlayerInventory.class, PlayerInventory::new)
                .append(new KeyedCodec<ItemContainer>("Armor", ItemContainer.CODEC),
                        (data, value) -> data.ARMOR = value,
                        data -> data.ARMOR).add()
                .append(new KeyedCodec<ItemContainer>("Backpack", ItemContainer.CODEC),
                        (data, value) -> data.BACKPACK = value,
                        data -> data.BACKPACK).add()
                .append(new KeyedCodec<ItemContainer>("Tool", ItemContainer.CODEC),
                        (data, value) -> data.TOOL = value,
                        data -> data.TOOL).add()
                .append(new KeyedCodec<ItemContainer>("Hotbar", ItemContainer.CODEC),
                        (data, value) -> data.HOTBAR = value,
                        data -> data.HOTBAR).add()
                .append(new KeyedCodec<ItemContainer>("Utility", ItemContainer.CODEC),
                        (data, value) -> data.UTILITY = value,
                        data -> data.UTILITY).add()
                .append(new KeyedCodec<ItemContainer>("Storage", ItemContainer.CODEC),
                        (data, value) -> data.STORAGE = value,
                        data -> data.STORAGE).add()
                .build();

        public PlayerInventory() {
            ARMOR = new SimpleItemContainer((short)1);
            BACKPACK = new SimpleItemContainer((short)1);
            TOOL = new SimpleItemContainer((short)1);
            HOTBAR = new SimpleItemContainer((short)1);
            UTILITY = new SimpleItemContainer((short)1);
            STORAGE = new SimpleItemContainer((short)1);
        }
        public PlayerInventory(ItemContainer ARMOR, ItemContainer BACKPACK, ItemContainer TOOL, ItemContainer HOTBAR, ItemContainer UTILITY, ItemContainer STORAGE) {
            this.ARMOR = ARMOR;
            this.BACKPACK = BACKPACK;
            this.TOOL = TOOL;
            this.HOTBAR = HOTBAR;
            this.UTILITY = UTILITY;
            this.STORAGE = STORAGE;
        }

        public static PlayerInventory fromPlayer(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
            return new PlayerInventory(
                    store.getComponent(ref, InventoryComponent.Armor.getComponentType()).getInventory().clone(),
                    store.getComponent(ref, InventoryComponent.Backpack.getComponentType()).getInventory().clone(),
                    store.getComponent(ref, InventoryComponent.Tool.getComponentType()).getInventory().clone(),
                    store.getComponent(ref, InventoryComponent.Hotbar.getComponentType()).getInventory().clone(),
                    store.getComponent(ref, InventoryComponent.Utility.getComponentType()).getInventory().clone(),
                    store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory().clone()
            );
        }
        public void applyToPlayer(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
            applyInventory(ARMOR, store.getComponent(ref, InventoryComponent.Armor.getComponentType()).getInventory());
            applyInventory(BACKPACK, store.getComponent(ref, InventoryComponent.Backpack.getComponentType()).getInventory());
            applyInventory(TOOL, store.getComponent(ref, InventoryComponent.Tool.getComponentType()).getInventory());
            applyInventory(HOTBAR, store.getComponent(ref, InventoryComponent.Hotbar.getComponentType()).getInventory());
            applyInventory(UTILITY, store.getComponent(ref, InventoryComponent.Utility.getComponentType()).getInventory());
            applyInventory(STORAGE, store.getComponent(ref, InventoryComponent.Storage.getComponentType()).getInventory());
        }
        private static void applyInventory(ItemContainer from, ItemContainer to) {
            var size = Math.min(from.getCapacity(), to.getCapacity());
            for (short i = 0; i < size; i++) {
                to.setItemStackForSlot(i, from.getItemStack(i));
            }
        }

    }

    public static final BuilderCodec<PlayerSkin> PLAYER_SKIN_CODEC;

    static {
        var builder = BuilderCodec.builder(PlayerSkin.class, PlayerSkin::new);
        builder.append(new KeyedCodec<>("BodyCharacteristic", BuilderCodec.STRING), (data, value) -> data.bodyCharacteristic = value, data -> data.bodyCharacteristic).add();
        builder.append(new KeyedCodec<>("Underwear", BuilderCodec.STRING), (data, value) -> data.underwear = value, data -> data.underwear).add();
        builder.append(new KeyedCodec<>("Face", BuilderCodec.STRING), (data, value) -> data.face = value, data -> data.face).add();
        builder.append(new KeyedCodec<>("Eyes", BuilderCodec.STRING), (data, value) -> data.eyes = value, data -> data.eyes).add();
        builder.append(new KeyedCodec<>("Ears", BuilderCodec.STRING), (data, value) -> data.ears = value, data -> data.ears).add();
        builder.append(new KeyedCodec<>("Mouth", BuilderCodec.STRING), (data, value) -> data.mouth = value, data -> data.mouth).add();
        builder.append(new KeyedCodec<>("FacialHair", BuilderCodec.STRING), (data, value) -> data.facialHair = value, data -> data.facialHair).add();
        builder.append(new KeyedCodec<>("Haircut", BuilderCodec.STRING), (data, value) -> data.haircut = value, data -> data.haircut).add();
        builder.append(new KeyedCodec<>("Eyebrows", BuilderCodec.STRING), (data, value) -> data.eyebrows = value, data -> data.eyebrows).add();
        builder.append(new KeyedCodec<>("Pants", BuilderCodec.STRING), (data, value) -> data.pants = value, data -> data.pants).add();
        builder.append(new KeyedCodec<>("Overpants", BuilderCodec.STRING), (data, value) -> data.overpants = value, data -> data.overpants).add();
        builder.append(new KeyedCodec<>("Undertop", BuilderCodec.STRING), (data, value) -> data.undertop = value, data -> data.undertop).add();
        builder.append(new KeyedCodec<>("Overtop", BuilderCodec.STRING), (data, value) -> data.overtop = value, data -> data.overtop).add();
        builder.append(new KeyedCodec<>("Shoes", BuilderCodec.STRING), (data, value) -> data.shoes = value, data -> data.shoes).add();
        builder.append(new KeyedCodec<>("HeadAccessory", BuilderCodec.STRING), (data, value) -> data.headAccessory = value, data -> data.headAccessory).add();
        builder.append(new KeyedCodec<>("FaceSccessory", BuilderCodec.STRING), (data, value) -> data.faceAccessory = value, data -> data.faceAccessory).add();
        builder.append(new KeyedCodec<>("EarAccessory", BuilderCodec.STRING), (data, value) -> data.earAccessory = value, data -> data.earAccessory).add();
        builder.append(new KeyedCodec<>("SkinFeature", BuilderCodec.STRING), (data, value) -> data.skinFeature = value, data -> data.skinFeature).add();
        builder.append(new KeyedCodec<>("Gloves", BuilderCodec.STRING), (data, value) -> data.gloves = value, data -> data.gloves).add();
        builder.append(new KeyedCodec<>("Cape", BuilderCodec.STRING), (data, value) -> data.cape = value, data -> data.cape).add();
        PLAYER_SKIN_CODEC = builder.build();
    }
}
