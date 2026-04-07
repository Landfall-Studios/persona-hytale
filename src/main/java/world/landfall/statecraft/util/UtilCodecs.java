package world.landfall.statecraft.util;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.PlayerSkin;

public class UtilCodecs {
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
