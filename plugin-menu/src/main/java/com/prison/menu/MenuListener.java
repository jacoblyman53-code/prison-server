package com.prison.menu;

import com.prison.menu.util.Gui;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * MenuListener — central router that dispatches GUI clicks to the correct handler.
 *
 * Every GUI in plugin-menu registers its title here. MenuPlugin forwards all
 * InventoryClickEvents whose title is known to this router.
 */
public class MenuListener {

    private final MenuPlugin plugin;

    public MenuListener(MenuPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns true if this listener should handle the given inventory title.
     */
    public boolean handles(Component title) {
        return isAny(title,
            MainMenuGUI.TITLE,
            MineBrowserGUI.TITLE,
            RankProgressionGUI.TITLE,
            SellCenterGUI.TITLE,
            PickaxeHomeGUI.TITLE,
            PickaxeEnchantsGUI.TITLE,
            PrestigeConfirmGUI.TITLE,
            PrestigeShopMenuGUI.TITLE,
            CratesHubGUI.TITLE,
            ShopCategoryPickerGUI.TITLE,
            ShopCategoryPageGUI.TITLE,
            ShopQuantityGUI.TITLE,
            BlackMarketMenuGUI.TITLE,
            QuestsMenuGUI.TITLE,
            GangHomeGUI.TITLE,
            WarpsMenuGUI.TITLE,
            KitsMenuGUI.TITLE,
            LeaderboardSelectorGUI.TITLE,
            CosmeticsMenuGUI.TITLE,
            SettingsGUI.TITLE,
            BoostsDetailGUI.TITLE
        );
    }

    /**
     * Dispatch a click event to the correct GUI handler.
     */
    public void onClick(Player player, int slot, ClickType click) {
        Component title = player.getOpenInventory().title();

        if (title.equals(MainMenuGUI.TITLE))            { MainMenuGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(MineBrowserGUI.TITLE))         { MineBrowserGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(RankProgressionGUI.TITLE))     { RankProgressionGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(SellCenterGUI.TITLE))          { SellCenterGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(PickaxeHomeGUI.TITLE))         { PickaxeHomeGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(PickaxeEnchantsGUI.TITLE))     { PickaxeEnchantsGUI.handleClick(player, slot, click, plugin); return; }
        if (title.equals(PrestigeConfirmGUI.TITLE))     { PrestigeConfirmGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(PrestigeShopMenuGUI.TITLE))    { PrestigeShopMenuGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(CratesHubGUI.TITLE))           { CratesHubGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(ShopCategoryPickerGUI.TITLE))  { ShopCategoryPickerGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(ShopCategoryPageGUI.TITLE))    { ShopCategoryPageGUI.handleClick(player, slot, click, plugin); return; }
        if (title.equals(ShopQuantityGUI.TITLE))        { ShopQuantityGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(BlackMarketMenuGUI.TITLE))     { BlackMarketMenuGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(QuestsMenuGUI.TITLE))          { QuestsMenuGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(GangHomeGUI.TITLE))            { GangHomeGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(WarpsMenuGUI.TITLE))           { WarpsMenuGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(KitsMenuGUI.TITLE))            { KitsMenuGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(LeaderboardSelectorGUI.TITLE)) { LeaderboardSelectorGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(CosmeticsMenuGUI.TITLE))       { CosmeticsMenuGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(SettingsGUI.TITLE))            { SettingsGUI.handleClick(player, slot, plugin); return; }
        if (title.equals(BoostsDetailGUI.TITLE))        { BoostsDetailGUI.handleClick(player, slot, plugin); return; }
    }

    // ----------------------------------------------------------------

    private static boolean isAny(Component title, Component... candidates) {
        for (Component c : candidates) {
            if (title.equals(c)) return true;
        }
        return false;
    }
}
