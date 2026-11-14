package hi.sierra.greedy_meshing.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional LambDynamicLights compatibility. That mod adds dynamic light by wrapping the vanilla
 * light lookup, which merged quads' own light sampling does not always go through, so its light
 * is added here directly. Reflection only; nothing of that mod is bundled.
 */
final class GreedyDynamicLightsCompat {
    private static final Object INSTANCE;
    private static final Object CONFIG_OWNER;
    private static final Method GET_LIGHTMAP;
    private static final Method GET_MODE;
    private static final Method IS_ENABLED;
    private static final Field CONFIG;

    static {
        Object instance = null;
        Method lightmap = null;
        Method mode = null;
        Method enabled = null;
        Field config = null;
        if (FabricLoader.getInstance().isModLoaded("lambdynlights")) {
            try {
                Class<?> ldl = Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
                instance = ldl.getMethod("get").invoke(null);
                for (Method m : ldl.getMethods()) {
                    if (m.getName().equals("getLightmapWithDynamicLight") && m.getParameterCount() == 3) {
                        lightmap = m;
                    }
                }
                config = ldl.getField("config");
                Class<?> cfg = config.getType();
                mode = cfg.getMethod("getDynamicLightsMode");
                enabled = mode.getReturnType().getMethod("isEnabled");
            } catch (Throwable t) {
                instance = null;
                lightmap = null;
            }
        }
        INSTANCE = instance;
        CONFIG_OWNER = instance;
        GET_LIGHTMAP = lightmap;
        GET_MODE = mode;
        IS_ENABLED = enabled;
        CONFIG = config;
    }

    private GreedyDynamicLightsCompat() {}

    static int apply(BlockAndTintGetter world, BlockState state, BlockPos pos, int light) {
        if (GET_LIGHTMAP == null || INSTANCE == null) return light;
        try {
            Object cfg = CONFIG.get(CONFIG_OWNER);
            if (!(Boolean) IS_ENABLED.invoke(GET_MODE.invoke(cfg))) return light;
            // Same condition as the mod's own hook: opaque blocks are not lit by it.
            //? if >=1.21.2 {
            if (state.isSolidRender()) return light;
            //?} else {
            /*if (state.isSolidRender(world, pos)) return light;
            *///?}
            return (Integer) GET_LIGHTMAP.invoke(INSTANCE, world, pos, light);
        } catch (Throwable t) {
            return light;
        }
    }
}
