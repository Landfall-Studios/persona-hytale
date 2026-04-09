package world.landfall.statecraft.api;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Instant;

/**
 *       "accountOwner": null,
 *       "id": 23,
 *       "isDeceased": false,
 *       "joinDate": 1759606886000,
 *       "name": "Octavia Dravik",
 *       "profilePictureUrl": "/uploads/profile-pictures/octaviadravik_1759623338_cb972849656241778a59acc70299d4b6_profile.png",
 *       "role": "officer",
 *       "usesDisplayName": true
 */
public class StatecraftNationMember {
    public long id;
    public boolean isDeceased;
    public Instant joinDate;
    public String name;
    public String profilePictureUrl;
    public String role;

    public static final BuilderCodec<StatecraftNationMember> CODEC = BuilderCodec.<StatecraftNationMember>builder(StatecraftNationMember.class, StatecraftNationMember::new)
            .append(new KeyedCodec<>("Id", BuilderCodec.LONG),
                    (data, value) ->data.id = value,
                    data -> data.id).add()
            .append(new KeyedCodec<>("IsDeceased", BuilderCodec.BOOLEAN),
                    (data, value) ->data.isDeceased = value,
                    data -> data.isDeceased).add()
            .append(new KeyedCodec<>("JoinDate", BuilderCodec.INSTANT),
                    (data, value) ->data.joinDate = value,
                    data -> data.joinDate).add()
            .append(new KeyedCodec<>("Name", BuilderCodec.STRING),
                    (data, value) ->data.name = value,
                    data -> data.name).add()
            .append(new KeyedCodec<>("ProfilePictureUrl", BuilderCodec.STRING),
                    (data, value) ->data.profilePictureUrl = value,
                    data -> data.profilePictureUrl).add()
            .append(new KeyedCodec<>("Role", BuilderCodec.STRING),
                    (data, value) ->data.role = value,
                    data -> data.role).add()
            .build();

    public StatecraftNationMember() {
        id = 0;
        isDeceased = false;
        joinDate = Instant.EPOCH;
        name = "";
        profilePictureUrl = "";
        role = "";
    }
    public StatecraftNationMember(long id, boolean isDeceased, Instant joinDate, String name, String profilePictureUrl, String role) {
        this.id = id;
        this.isDeceased = isDeceased;
        this.joinDate = joinDate;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
        this.role = role;
    }
}
