package world.landfall.statecraft.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;
import world.landfall.statecraft.api.StatecraftCharacter;

import java.util.UUID;

public class CharacterComponent implements Component<EntityStore> {
    public StatecraftCharacter character;

    public static final BuilderCodec<CharacterComponent> CODEC =
            BuilderCodec.<CharacterComponent>builder(CharacterComponent.class, CharacterComponent::new)
                    .append(new KeyedCodec<>("Character", StatecraftCharacter.CODEC),
                            (data, value) -> data.character = value,
                            data -> data.character).add()
                    .build();

    public CharacterComponent() {
        character = new StatecraftCharacter(0, "", UUID.randomUUID());
    }
    private CharacterComponent(CharacterComponent component) {
        character = component.character;
    }
    
    @Override
    public @Nullable Component<EntityStore> clone() {
        return new CharacterComponent(this);
    }

}
