package world.landfall.statecraft.api;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.time.Instant;

/**
 *     This defines a nation as will be returned by the ScC API
 */
public class StatecraftNation {
    public String abbreviation;
    public int activeMembersCount;
    public int activityPercentage;
    public int citizenCount;
    public Instant creationTime;
    public String description;
    public long id;
    public boolean isActive;
    public String name;
    public String nationalCurrency;
    public int officerCount;
    public String profilePictureUrl;
    public int totalMembers;

    public static final BuilderCodec<StatecraftNation> CODEC = BuilderCodec.<StatecraftNation>builder(StatecraftNation.class, StatecraftNation::new)
            .append(new KeyedCodec<>("Abbreviation", BuilderCodec.STRING),
                    (data, value) -> data.abbreviation = value,
                    data -> data.abbreviation).add()
            .append(new KeyedCodec<>("ActiveMembersCount", BuilderCodec.INTEGER),
                    (data, value) -> data.activeMembersCount = value,
                    data -> data.activeMembersCount).add()
            .append(new KeyedCodec<>("ActivityPercentage", BuilderCodec.INTEGER),
                    (data, value) -> data.activityPercentage = value,
                    data -> data.activityPercentage).add()
            .append(new KeyedCodec<>("CitizenCount", BuilderCodec.INTEGER),
                    (data, value) -> data.citizenCount = value,
                    data -> data.citizenCount).add()
            .append(new KeyedCodec<>("CreationTime", BuilderCodec.INSTANT),
                    (data, value) -> data.creationTime = value,
                    data -> data.creationTime).add()
            .append(new KeyedCodec<>("Description", BuilderCodec.STRING),
                    (data, value) -> data.description = value,
                    data -> data.description).add()
            .append(new KeyedCodec<>("ID", BuilderCodec.LONG),
                    (data, value) -> data.id = value,
                    data -> data.id).add()
            .append(new KeyedCodec<>("IsActive", BuilderCodec.BOOLEAN),
                    (data, value) -> data.isActive = value,
                    data -> data.isActive).add()
            .append(new KeyedCodec<>("Name", BuilderCodec.STRING),
                    (data, value) -> data.name = value,
                    data -> data.name).add()
            .append(new KeyedCodec<>("NationalCurrency", BuilderCodec.STRING),
                    (data, value) -> data.nationalCurrency = value,
                    data -> data.nationalCurrency).add()
            .append(new KeyedCodec<>("OfficerCount", BuilderCodec.INTEGER),
                    (data, value) -> data.officerCount = value,
                    data -> data.officerCount).add()
            .append(new KeyedCodec<>("ProfilePictureUrl", BuilderCodec.STRING),
                    (data, value) -> data.profilePictureUrl = value,
                    data -> data.profilePictureUrl).add()
            .append(new KeyedCodec<>("TotalMembers", BuilderCodec.INTEGER),
                    (data, value) -> data.totalMembers = value,
                    data -> data.totalMembers).add()
            .build();

    public StatecraftNation() {
        abbreviation = "";
        activeMembersCount = 0;
        activityPercentage = 0;
        citizenCount = 0;
        creationTime = Instant.EPOCH;
        description = "";
        id = 0;
        isActive = false;
        name = "";
        nationalCurrency = "";
        officerCount = 0;
        profilePictureUrl = "";
        totalMembers = 0;

    }
    public StatecraftNation(String abbreviation, int activeMembersCount, int activityPercentage,
                            int citizenCount, Instant creationTime, String description,
                            long id, boolean isActive, String name,
                            String nationalCurrency, int officerCount, String profilePictureUrl,
                            int totalMembers) {
        this.abbreviation = abbreviation;
        this.activeMembersCount = activeMembersCount;
        this.activityPercentage = activityPercentage;
        this.citizenCount = citizenCount;
        this.creationTime = creationTime;
        this.description = description;
        this.id = id;
        this.isActive = isActive;
        this.name = name;
        this.nationalCurrency = nationalCurrency;
        this.officerCount = officerCount;
        this.profilePictureUrl = profilePictureUrl;
        this.totalMembers = totalMembers;
    }

}
