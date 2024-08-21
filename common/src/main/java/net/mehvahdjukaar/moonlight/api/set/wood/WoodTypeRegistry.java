package net.mehvahdjukaar.moonlight.api.set.wood;

import net.mehvahdjukaar.moonlight.api.events.AfterLanguageLoadEvent;
import net.mehvahdjukaar.moonlight.api.set.BlockTypeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WoodTypeRegistry extends BlockTypeRegistry<WoodType> {

    public static final WoodType OAK_TYPE = new WoodType(ResourceLocation.parse("oak"), Blocks.OAK_PLANKS, Blocks.OAK_LOG);

    public static final WoodTypeRegistry INSTANCE = new WoodTypeRegistry();

    public static Collection<WoodType> getTypes() {
        return INSTANCE.getValues();
    }

    @Nullable
    public static WoodType getValue(ResourceLocation name) {
        return INSTANCE.get(name);
    }

    public static WoodType fromNBT(String name) {
        return INSTANCE.getFromNBT(name);
    }

    public static WoodType fromVanilla(net.minecraft.world.level.block.state.properties.WoodType vanillaType) {
        return INSTANCE.getFromVanilla(vanillaType);
    }

    //instance stuff

    private final Map<net.minecraft.world.level.block.state.properties.WoodType, WoodType> fromVanilla = new IdentityHashMap<>();

    public WoodTypeRegistry() {
        super(WoodType.class, "wood_type");
        this.addFinder(() -> {
            var b = new WoodType(ResourceLocation.parse("bamboo"), Blocks.BAMBOO_PLANKS, Blocks.BAMBOO_BLOCK);
            b.addChild("stripped_log", Blocks.STRIPPED_BAMBOO_BLOCK);
            return Optional.of(b);
        });
    }

    @Override
    public WoodType getDefaultType() {
        return OAK_TYPE;
    }

    public static Set<String> IGNORED_MODS = new HashSet<>(Set.of("chipped", "securitycraft", "absentbydesign", "immersive_weathering"));

    //returns if this block is the base plank block
    @Override
    public Optional<WoodType> detectTypeFromBlock(Block baseBlock, ResourceLocation baseRes) {
        String name = null;
        String path = baseRes.getPath();
        // Support TerraFirmaCraft (TFC) & ArborFirmaCraft (AFC)
        if (baseRes.getNamespace().equals("tfc") || baseRes.getNamespace().equals("afc")) {
            // Needs to contain palnks in its path
            if (path.contains("wood/planks/")) {
                var log = BuiltInRegistries.BLOCK.getOptional(
                        baseRes.withPath(path.replace("planks", "log")));
                if (log.isPresent()) {
                    ResourceLocation id = baseRes.withPath(path.replace("wood/planks/", ""));
                    return Optional.of(new WoodType(id, baseBlock, log.get()));
                }
            }
            return Optional.empty();
        }
        // DEFAULT
        if (path.endsWith("_planks")) { //needs to contain planks in its name
            name = path.substring(0, path.length() - "_planks".length());
        } else if (path.startsWith("planks_")) {
            name = path.substring("planks_".length());
        } else if (path.endsWith("_plank")) {
            name = path.substring(0, path.length() - "_plank".length());
        } else if (path.startsWith("plank_")) {
            name = path.substring("plank_".length());
        }
        String namespace = baseRes.getNamespace();
        if (name != null && !IGNORED_MODS.contains(namespace)) {

            BlockState state = baseBlock.defaultBlockState();
            //Can't check if the block is a full one, so I do this. Adding some checks here
            if (state.getProperties().size() <= 2 && !(baseBlock instanceof SlabBlock)) {
                //needs to use wood sound type
                //we do not allow "/" in the wood name
                name = name.replace("/", "_");
                ResourceLocation id = baseRes.withPath(name);
                Block logBlock = findLog(id);
                if (logBlock != null) {
                    return Optional.of(new WoodType(id, baseBlock, logBlock));
                }
            }
        }
        return Optional.empty();
    }

    @Nullable
    private static Block findLog(ResourceLocation id) {
        List<String> keywords = List.of("log", "stem", "stalk", "hyphae");
        List<ResourceLocation> resources = new ArrayList<>();
        for (String keyword : keywords) {
            resources.add(id.withPath(id.getPath() + "_" + keyword));
            resources.add(id.withPath(keyword + "_" + id.getPath()));
            resources.add(ResourceLocation.parse(id.getPath() + "_" + keyword));
            resources.add(ResourceLocation.parse(keyword + "_" + id.getPath()));
        }
        ResourceLocation[] test = resources.toArray(new ResourceLocation[0]);
        Block temp = null;
        for (var r : test) {
            if (BuiltInRegistries.BLOCK.containsKey(r)) {
                temp = BuiltInRegistries.BLOCK.get(r);
                break;
            }
        }
        return temp;
    }

    @Override
    public void addTypeTranslations(AfterLanguageLoadEvent language) {
        getValues().forEach((w) -> {
            if (language.isDefault()) language.addEntry(w.getTranslationKey(), w.getReadableName());
        });
    }

    @Nullable
    public WoodType getFromVanilla(net.minecraft.world.level.block.state.properties.WoodType woodType) {
        if (fromVanilla.isEmpty()) {
            for (WoodType w : getValues()) {
                var vanilla = w.toVanilla();
                if (vanilla != null) fromVanilla.put(vanilla, w);
            }
        }
        return fromVanilla.get(woodType);
    }

    @Override
    protected void finalizeAndFreeze() {
        // order according vanilla
        List<String> vanillaOrder = List.of(
                "minecraft:oak",
                "minecraft:spruce",
                "minecraft:birch",
                "minecraft:jungle",
                "minecraft:acacia",
                "minecraft:dark_oak",
                "minecraft:mangrove",
                "minecraft:cherry",
                "minecraft:bamboo",
                "minecraft:crimson",
                "minecraft:warped"
        );
        List<WoodType> temp = new ArrayList<>(builder);
        builder.clear();
        outer:
        for (var v : vanillaOrder) {
            for (var t : temp) {
                if (t.getId().toString().equals(v)) {
                    builder.add(t);
                    temp.remove(t);
                    continue outer;
                }
            }
        }
        builder.addAll(temp);
        super.finalizeAndFreeze();
    }
}
