package net.mehvahdjukaar.moonlight.core.mixins;

import io.netty.buffer.Unpooled;
import net.mehvahdjukaar.moonlight.api.map.CustomMapData;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDecorationRegistry;
import net.mehvahdjukaar.moonlight.api.map.type.MapDecorationType;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.moonlight.core.Moonlight;
import net.mehvahdjukaar.moonlight.core.misc.IMapDataPacketExtension;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

//I hope this won't break with mods. We need this as all data needs to be received at the same time
@Mixin(ClientboundMapItemDataPacket.class)
public class MapItemDataPacketMixin implements IMapDataPacketExtension {

    @Shadow
    @Final
    @Nullable
    private MapItemSavedData.MapPatch colorPatch;
    @Unique
    private CustomMapDecoration[] moonlight$customDecorations = null;
    @Unique
    private CustomMapData[] moonlight$customData = null;
    @Unique
    private int moonlight$mapCenterX = 0;
    @Unique
    private int moonlight$mapCenterZ = 0;
    @Unique
    private ResourceLocation moonlight$dimension = Level.OVERWORLD.location();

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
            at = @At("RETURN"))
    private void readExtraData(FriendlyByteBuf buf, CallbackInfo ci) {
        //we always need to send enough data to create the correct map type because we dont know if client has it
        if(buf.readBoolean()) {
            moonlight$dimension = buf.readResourceLocation();
            moonlight$mapCenterX = buf.readVarInt();
            moonlight$mapCenterZ = buf.readVarInt();
        }
        if (buf.readBoolean()) {
            this.moonlight$customDecorations = new CustomMapDecoration[buf.readVarInt()];
            for (int m = 0; m < moonlight$customDecorations.length; ++m) {
                MapDecorationType<?, ?> type = MapDecorationRegistry.get(buf.readResourceLocation());
                if (type != null) {
                    moonlight$customDecorations[m] = type.loadDecorationFromBuffer(buf);
                }
            }
        }
        if (buf.readBoolean()) {
            //TODO: I really could have merged the 2 systems
            this.moonlight$customData = new CustomMapData[buf.readVarInt()];
            for (int m = 0; m < moonlight$customData.length; ++m) {
                CustomMapData.Type<?> type = MapDecorationRegistry.CUSTOM_MAP_DATA_TYPES.getOrDefault(buf.readResourceLocation(), null);
                if (type != null) {
                    moonlight$customData[m] = type.createFromBuffer(buf);
                }
            }
        }
    }

    //new constructor expansion
    @Inject(method = "<init>(IBZLjava/util/Collection;Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData$MapPatch;)V",
            at = @At("RETURN"))
    private void addExtraCenterAndDimension(int mapId, byte b, boolean bl, Collection collection, MapItemSavedData.MapPatch mapPatch, CallbackInfo ci) {
        var level = PlatHelper.getCurrentServer().getLevel(Level.OVERWORLD);
        moonlight$dimension = null;
        if (level != null) {
            MapItemSavedData data = level.getMapData(MapItem.makeKey(mapId));
            if(data != null) {
                this.moonlight$mapCenterX = data.centerX;
                this.moonlight$mapCenterZ = data.centerZ;
                this.moonlight$dimension = data.dimension.location();
            }
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void writeExtraData(FriendlyByteBuf buf, CallbackInfo ci) {
        buf.writeBoolean(moonlight$dimension != null);
        if(moonlight$dimension != null) {
            buf.writeResourceLocation(moonlight$dimension);
            buf.writeVarInt(moonlight$mapCenterX);
            buf.writeVarInt(moonlight$mapCenterZ);
        }

        buf.writeBoolean(moonlight$customDecorations != null);
        if (moonlight$customDecorations != null) {
            buf.writeVarInt(moonlight$customDecorations.length);

            for (CustomMapDecoration decoration : moonlight$customDecorations) {
                buf.writeResourceLocation(Utils.getID(decoration.getType()));
                decoration.saveToBuffer(buf);
            }
        }

        buf.writeBoolean(moonlight$customData != null);
        if (moonlight$customData != null) {
            buf.writeVarInt(this.moonlight$customData.length);

            for (CustomMapData data : moonlight$customData) {
                buf.writeResourceLocation(data.getType().id());
                data.saveToBuffer(buf);
            }
        }
    }

    @Override
    public void moonlight$sendCustomDecorations(Collection<CustomMapDecoration> decorations) {

        //packet will be passed to client no decoding. if we are on an integrated server we need to create new objects
        if (PlatHelper.getPhysicalSide().isClient()) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            decorations = decorations.stream().map(e -> {
                e.saveToBuffer(buffer);
                CustomMapDecoration d = e.getType().loadDecorationFromBuffer(buffer);
                return d;
            }).toList();
        }
        moonlight$customDecorations = decorations.toArray(CustomMapDecoration[]::new);
    }

    @Override
    public void moonlight$sendCustomMapData(Collection<CustomMapData> data) {

        //clone objects
        if (PlatHelper.getPhysicalSide().isClient()) {
            data = data.stream().map(e -> {
                CompoundTag tag = new CompoundTag();
                e.save(tag);
                CustomMapData n = e.getType().factory().apply(tag);
                return n;
            }).toList();
        }
        moonlight$customData = data.toArray(CustomMapData[]::new);
    }

    @Override
    public CustomMapData[] moonlight$getCustomMapData() {
        return moonlight$customData;
    }

    @Override
    public CustomMapDecoration[] moonlight$getCustomDecorations() {
        return moonlight$customDecorations;
    }

    @Override
    public Vector2i moonlight$getMapCenter() {
        return new Vector2i(moonlight$mapCenterX, moonlight$mapCenterZ);
    }

    @Override
    public MapItemSavedData.MapPatch moonlight$getColorPatch() {
        return colorPatch;
    }

    @Override
    public ResourceKey<Level> moonlight$getDimension() {
        return ResourceKey.create(Registries.DIMENSION, moonlight$dimension);
    }
}