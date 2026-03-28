package com.prison.shop;

import org.bukkit.Material;
import java.util.List;

public record ShopCategory(
    String id,
    String displayName,   // MiniMessage string
    Material icon,
    List<ShopItem> items
) {}
