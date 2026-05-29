package com.mugetsu.agent.mapping;

import java.util.Map;

/**
 * Known class/method/field names across Forge MCP, Fabric Yarn, and Fabric Intermediary.
 * Entries are listed in priority order (most likely first).
 */
public final class KnownMappings {

    // Class name candidates — logical key → [candidate dot-names...]
    public static final Map<String, String[]> CLASSES = Map.ofEntries(
        Map.entry("Minecraft", new String[]{
            "net.minecraft.client.Minecraft",           // Forge / official
            "net.minecraft.client.MinecraftClient",     // Fabric Yarn
            "net.minecraft.class_310"                   // Fabric intermediary
        }),
        Map.entry("LocalPlayer", new String[]{
            "net.minecraft.client.player.LocalPlayer",              // Forge 1.17+
            "net.minecraft.client.entity.player.ClientPlayerEntity",// Forge 1.16
            "net.minecraft.client.network.ClientPlayerEntity",      // Fabric Yarn
            "net.minecraft.class_746"                               // Fabric intermediary (Entity base)
        }),
        Map.entry("GameRenderer", new String[]{
            "net.minecraft.client.renderer.GameRenderer",   // Forge 1.17+
            "net.minecraft.client.render.GameRenderer",     // Fabric Yarn
            "net.minecraft.class_757"                       // Fabric intermediary
        }),
        Map.entry("LevelRenderer", new String[]{
            "net.minecraft.client.renderer.LevelRenderer",  // Forge 1.17+
            "net.minecraft.client.render.WorldRenderer",    // Fabric Yarn
            "net.minecraft.class_761"                       // Fabric intermediary
        }),
        Map.entry("InteractionManager", new String[]{
            "net.minecraft.client.multiplayer.MultiPlayerGameMode",         // Forge 1.17+
            "net.minecraft.client.network.ClientPlayerInteractionManager",  // Fabric Yarn
            "net.minecraft.class_1234"                                       // Fabric intermediary
        }),
        Map.entry("Entity", new String[]{
            "net.minecraft.world.entity.Entity",    // Forge 1.17+
            "net.minecraft.entity.Entity",          // Fabric Yarn / Forge 1.16
            "net.minecraft.class_1297"              // Fabric intermediary
        }),
        Map.entry("LivingEntity", new String[]{
            "net.minecraft.world.entity.LivingEntity",
            "net.minecraft.entity.LivingEntity",
            "net.minecraft.class_1309"
        }),
        Map.entry("AABB", new String[]{
            "net.minecraft.world.phys.AABB",    // Forge 1.17+
            "net.minecraft.util.math.Box",      // Fabric Yarn
            "net.minecraft.class_238"           // Fabric intermediary
        }),
        Map.entry("InGameHud", new String[]{
            "net.minecraft.client.gui.Gui",                 // Forge 1.17+
            "net.minecraft.client.gui.hud.InGameHud",       // Fabric Yarn
            "net.minecraft.client.gui.DrawableHelper",      // older Fabric
            "net.minecraft.class_332"                       // Fabric intermediary
        }),
        Map.entry("Options", new String[]{
            "net.minecraft.client.Options",         // Forge 1.17+
            "net.minecraft.client.option.GameOptions", // Fabric Yarn
            "net.minecraft.class_315"               // Fabric intermediary
        }),
        Map.entry("KeyboardHandler", new String[]{
            "net.minecraft.client.KeyboardHandler",         // Forge 1.17+
            "net.minecraft.client.Keyboard",                // Fabric Yarn
            "net.minecraft.class_309"                       // Fabric intermediary
        })
    );

    // Method name candidates — "ClassName.logical" → [candidate names...]
    public static final Map<String, String[]> METHODS = Map.ofEntries(
        Map.entry("Entity.travel", new String[]{
            "travel",           // Forge / official
            "method_18866",     // Fabric intermediary 1.16-1.19
            "method_30632"      // Fabric intermediary 1.20+
        }),
        Map.entry("Entity.causeFallDamage", new String[]{
            "causeFallDamage",  // Forge
            "handleFallDamage", // Forge alt
            "method_6005",      // Fabric intermediary
            "method_30546"      // Fabric intermediary 1.20+
        }),
        Map.entry("Entity.getBoundingBox", new String[]{
            "getBoundingBox",   // Forge
            "getBoundingBox",   // same in Fabric
            "method_5829",      // Fabric intermediary
            "getEntityBoundingBox" // older Forge
        }),
        Map.entry("GameRenderer.renderLevel", new String[]{
            "renderLevel",      // Forge 1.17+
            "render",           // Fabric Yarn
            "method_3153",      // Fabric intermediary
            "renderWorld"       // Forge 1.16
        }),
        Map.entry("InteractionManager.getPickRange", new String[]{
            "getPickRange",         // Forge 1.19+
            "getAttackRange",       // Forge 1.17-1.18
            "getReachDistance",     // older
            "method_2830",          // Fabric intermediary
            "interactionManager"    // field
        }),
        Map.entry("InteractionManager.attack", new String[]{
            "attack",           // Forge
            "attackEntity",     // Fabric Yarn
            "method_2879",      // Fabric intermediary
            "func_78768_b"      // old Forge
        }),
        Map.entry("InGameHud.render", new String[]{
            "render",           // Forge + Fabric
            "renderHotbar",     // fallback
            "method_1744"       // Fabric intermediary
        }),
        Map.entry("KeyboardHandler.onKey", new String[]{
            "onKey",            // Forge 1.17+ and Fabric Yarn
            "keyPress",         // Forge alt name
            "method_1454",      // Fabric intermediary 1.16-1.20
            "method_22673"      // Fabric intermediary 1.21+
        })
    );

    // Field name candidates — "ClassName.logical" → [candidate names...]
    public static final Map<String, String[]> FIELDS = Map.ofEntries(
        Map.entry("Minecraft.player", new String[]{
            "player", "field_1724", "f_91074_"
        }),
        Map.entry("Minecraft.gameRenderer", new String[]{
            "gameRenderer", "field_1763", "f_90975_"
        }),
        Map.entry("Minecraft.levelRenderer", new String[]{
            "levelRenderer", "worldRenderer", "field_1761", "f_90995_"
        }),
        Map.entry("Minecraft.gameMode", new String[]{
            "gameMode", "interactionManager", "field_1761", "f_91089_"
        }),
        Map.entry("Minecraft.options", new String[]{
            "options", "gameSettings", "field_71474_y", "f_91058_"
        }),
        Map.entry("Minecraft.window", new String[]{
            "window", "mainWindow", "field_1754"
        }),
        Map.entry("Options.gamma", new String[]{
            "gamma", "field_74334_X", "f_92080_"
        })
    );

    private KnownMappings() {}
}
