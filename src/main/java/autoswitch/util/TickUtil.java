package autoswitch.util;

import static autoswitch.AutoSwitch.featureCfg;
import static autoswitch.AutoSwitch.tickTime;

import autoswitch.AutoSwitch;
import autoswitch.config.AutoSwitchConfig;
import autoswitch.events.SwitchEvent;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;

public class TickUtil {

    /**
     * Update toggle and send message to player if necessary
     *
     * @param player      client's player
     * @param toggle      toggle to update on keybinding press
     * @param keyChooser  choose which translation to use
     * @param tlKeyTruthy message if keyChooser is true
     * @param tlKeyFalsy  message if keyChooser is false
     *
     * @return updated toggle
     */
    public static boolean keybindingToggleAction(PlayerEntity player, boolean toggle, boolean keyChooser,
                                                 String tlKeyTruthy, String tlKeyFalsy) {
        if (featureCfg.toggleMessageControl() != AutoSwitchConfig.DisplayControl.OFF) {
            //Toggle message
            TranslatableText msg = new TranslatableText(keyChooser ? tlKeyTruthy : tlKeyFalsy);

            //Display msg above hotbar, set false to display in text chat
            player.sendMessage(msg, featureCfg.toggleMessageControl() == AutoSwitchConfig.DisplayControl.DEFAULT);
        }

        return !toggle;
    }

    /**
     * Tick the scheduler's clock and schedule switchback
     *
     * @param player client player
     */
    public static void tickEventSchedule(PlayerEntity player) {
        if (player == null) return; // Ensure nothing bad happens

        // Schedule switchback iff it is needed
        EventUtil.eventHandler(tickTime, 0, SwitchEvent.SWITCHBACK.setPlayer(player));

        // Tick event system clock
        tickTime += 1;
        AutoSwitch.scheduler.execute(tickTime);
    }

}
