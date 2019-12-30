package autoswitch;

import net.fabricmc.fabric.api.tools.FabricToolTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@SuppressWarnings("WeakerAccess")
abstract class Targetable {
    HashMap<String, ArrayList<Object>> toolTargetLists;
    LinkedHashMap<String, ArrayList<Integer>> toolLists;
    PlayerEntity player;

    public Targetable(PlayerEntity player, AutoSwitchConfig cfg, AutoSwitchMaterialConfig matCfg) {
        toolTargetLists = new AutoSwitchLists(cfg, matCfg).getToolTargetLists();
        toolLists = new AutoSwitchLists(cfg, matCfg).getToolLists();
        this.player = player;
    }

    static Targetable of(Entity target, PlayerEntity player, AutoSwitchConfig cfg, AutoSwitchMaterialConfig matCfg) {
        return new TargetableEntity(target, player, cfg, matCfg);
    }

    static Targetable of(BlockState target, PlayerEntity player, AutoSwitchConfig cfg, AutoSwitchMaterialConfig matCfg) {
        return new TargetableMaterial(target, player, cfg, matCfg);
    }

    static Targetable of(int prevSlot, PlayerEntity player) {
        return new TargetableNone(prevSlot, player);
    }

    //populate all of the tool lists
    public void populateToolLists(PlayerEntity player) {
        List<ItemStack> hotbar = player.inventory.main.subList(0, 9);
        for (int i=0; i<9; i++) {
            populateCommonToolList(hotbar.get(i), i);
            populateTargetTools(hotbar.get(i), i);

        }

    }

    //Populate Common Tool Lists
    private void populateCommonToolList(ItemStack stack, int i) {
        Item item = stack.getItem();
        if (FabricToolTags.AXES.contains(item) || item instanceof AxeItem) {
            this.toolLists.get("axes").add(i);
            //Genning these in common as there's no good way to do it separately without repeating the axe check
            if (EnchantmentHelper.getLevel(Enchantments.FORTUNE, stack) > 0){
                this.toolLists.get("fortAxes").add(i);
            }
            if (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, stack) > 0){
                this.toolLists.get("silkAxes").add(i);
            }
        } else if (FabricToolTags.SWORDS.contains(item) || item instanceof SwordItem) {
            this.toolLists.get("swords").add(i);
        }

    }

    //Change the player's selected tool
    public Optional<Boolean> changeTool() {
        int currentSlot = this.player.inventory.selectedSlot;
        Optional<Integer> slot = findSlot();
        if (slot.isPresent()) {
            int intSlot = slot.get();
            if (intSlot == currentSlot) {
                //No need to change slot!
                return Optional.of(false);
            }

            //Loop over it since scrollinhotbar only moves one pos
            for (int i = Math.abs(currentSlot - intSlot); i > 0; i--){
                this.player.inventory.scrollInHotbar(currentSlot - intSlot);
            }
            return Optional.of(true); //Slot changed
        }

        return Optional.empty();

    }


    //Overrides
    abstract void populateTargetTools(ItemStack stack, int i);

    abstract Optional<Integer> findSlot();


}

@SuppressWarnings("WeakerAccess")
class TargetableNone extends Targetable {
    int prevSlot;


    public TargetableNone(int prevSlot, PlayerEntity player) {
        super(player, null, null);
        this.prevSlot = (int) prevSlot;
    }

    @Override
    void populateTargetTools(ItemStack stack, int i) {

    }

    @Override
    Optional<Integer> findSlot() {
        return Optional.of(this.prevSlot);
    }
}

@SuppressWarnings("WeakerAccess")
class TargetableEntity extends Targetable {
    private final Entity entity;

    public TargetableEntity(Entity target, PlayerEntity player, AutoSwitchConfig cfg, AutoSwitchMaterialConfig matCfg) {
        super(player, cfg, matCfg);
        populateToolLists(player);
        this.entity = target;
        this.player = player;
    }


    @Override
    void populateTargetTools(ItemStack stack, int i) {
        Item item = stack.getItem();
        if (EnchantmentHelper.getLevel(Enchantments.BANE_OF_ARTHROPODS, stack) > 0){
            this.toolLists.get("banes").add(i);
        } else if (EnchantmentHelper.getLevel(Enchantments.SMITE, stack) > 0){
            this.toolLists.get("smites").add(i);
        } else if (EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack) > 0){
            this.toolLists.get("sharps").add(i);
        } else if (EnchantmentHelper.getLevel(Enchantments.IMPALING, stack) > 0){
            this.toolLists.get("impalingTridents").add(i);
        }
        if (item instanceof TridentItem) {
            this.toolLists.get("tridents").add(i);
        }

    }

    @Override
    Optional<Integer> findSlot() {
        if (entity instanceof LivingEntity) {
            if (((LivingEntity) entity).getGroup() == EntityGroup.ARTHROPOD) {
                if (!toolLists.get("banes").isEmpty()) {
                    return Optional.of(toolLists.get("banes").get(0));
                }
            }

            if (((LivingEntity) entity).getGroup() == EntityGroup.UNDEAD) {
                if (!toolLists.get("smites").isEmpty()){
                    return Optional.of(toolLists.get("smites").get(0));
                }
            }

            if (((LivingEntity) entity).getGroup() == EntityGroup.AQUATIC) {
                if (!toolLists.get("impalingTridents").isEmpty()){
                    return Optional.of(toolLists.get("impalingTridents").get(0));
                }
            }

        }

        if (entity instanceof BoatEntity) {
            if (!toolLists.get("axes").isEmpty()) {
                return Optional.of(toolLists.get("axes").get(0));
            }
        }

        for (Map.Entry<String, ArrayList<Integer>> toolList : toolLists.entrySet()){
            if (!toolList.getValue().isEmpty()) {
                return Optional.of(toolList.getValue().get(0));
            }
        }

        return Optional.empty();
    }
}

@SuppressWarnings("WeakerAccess")
class TargetableMaterial extends Targetable {
    private final Material target;

    public TargetableMaterial(BlockState target, PlayerEntity player, AutoSwitchConfig cfg, AutoSwitchMaterialConfig matCfg) {
        super(player, cfg, matCfg);
        populateToolLists(player);
        this.player = player;
        this.target = target.getMaterial();
    }


    @Override
    void populateTargetTools(ItemStack stack, int i) {
        Item item = stack.getItem();
        if (FabricToolTags.PICKAXES.contains(item) || item instanceof PickaxeItem) {
            this.toolLists.get("picks").add(i);
            if (EnchantmentHelper.getLevel(Enchantments.FORTUNE, stack) > 0){
                this.toolLists.get("fortPicks").add(i);
            }
            if (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, stack) > 0){
                this.toolLists.get("silkPicks").add(i);
            }
        } else if (FabricToolTags.SHOVELS.contains(item) || item instanceof ShovelItem) {
            this.toolLists.get("shovels").add(i);
            if (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, stack) > 0){
                this.toolLists.get("silkShovels").add(i);
            }
        } else if (item instanceof ShearsItem) {
            this.toolLists.get("shears").add(i);
        }

    }

    @Override
    Optional<Integer> findSlot() {
        for (Map.Entry<String, ArrayList<Integer>> toolList : toolLists.entrySet()){
            if (!toolList.getValue().isEmpty()) {
                if (!toolTargetLists.get(StringUtils.chop(toolList.getKey())).isEmpty()) {
                    if (toolTargetLists.get(StringUtils.chop(toolList.getKey())).contains(target)) {
                        return Optional.of(toolList.getValue().get(0));
                    }
                }

            }
        }

        return Optional.empty();
    }
}