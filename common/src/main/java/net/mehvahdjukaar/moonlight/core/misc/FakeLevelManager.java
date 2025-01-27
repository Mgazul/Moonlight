package net.mehvahdjukaar.moonlight.core.misc;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.mehvahdjukaar.moonlight.core.Moonlight;
import net.mehvahdjukaar.moonlight.core.fluid.SoftFluidInternal;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class FakeLevelManager {

    protected static final Map<String, Level> INSTANCES = new Object2ObjectArrayMap<>();

    @ApiStatus.Internal
    @VisibleForTesting
    public static Collection<Level> invalidateAll() {
        var toReturn = INSTANCES.values();
        INSTANCES.clear();
        return toReturn;
    }

    @ApiStatus.Internal
    public static void invalidate(String name) {
        INSTANCES.remove(name);
    }

    public static FakeLevel getDefaultClient(Level original) {
        return getClient("dummy_world", original, FakeLevel::new);
    }

    public static <T extends FakeLevel> T getClient(String id, Level original, BiFunction<String, RegistryAccess, FakeLevel> constructor) {
        id = "client_" + id;
        String finalId = id;
        return (T) INSTANCES.computeIfAbsent(id, k -> constructor.apply(finalId, original.registryAccess()));
    }


    public static FakeServerLevel getDefaultServer(ServerLevel original) {
        return getServer("dummy_world", original, FakeServerLevel::new);
    }

    public static <T extends FakeServerLevel> T getServer(String id, ServerLevel original, BiFunction<String, ServerLevel, FakeServerLevel> constructor) {
        id = "server_" + id;
        String finalId = id;
        return (T) INSTANCES.computeIfAbsent(id, k -> constructor.apply(finalId, original));
    }

    public static Level get(String id, Level original,
                            BiFunction<String, RegistryAccess, FakeLevel> clientConstr,
                            BiFunction<String, ServerLevel, FakeServerLevel> serverConstr) {
        if (original instanceof ServerLevel sl) {
            return getServer(id, sl, serverConstr);
        } else {
            return getClient(id, original, clientConstr);
        }
    }

    public static Level getDefault(Level original) {
        if (original instanceof ServerLevel sl) {
            return getDefaultServer(sl);
        } else {
            return getDefaultClient(original);
        }
    }

    public interface ILevelLike {
        Level cast();
    }
}
