package world.landfall.statecraft.util;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MapCodec <K, V> implements Codec<Map<K, V>> {
    private final Codec<K> kCodec;
    private final Codec<V> vCodec;

    public MapCodec(Codec<K> kCodec, Codec<V> vCodec) {
        this.kCodec = kCodec;
        this.vCodec = vCodec;
    }
    @Override
    public @Nullable Map<K, V> decode(BsonValue bsonValue, ExtraInfo extraInfo) {
        try {
            var map = new HashMap<K, V>();
            for (var x : ((BsonArray)bsonValue).stream().toList()) {
                var k = kCodec.decode(x.asArray().get(0), extraInfo);
                var v = vCodec.decode(x.asArray().get(1), extraInfo);
                map.put(k, v);
            }
            return map;
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Override
    public BsonValue encode(Map<K, V> kvMap, ExtraInfo extraInfo) {
        var entries = kvMap.entrySet();
        var bsonEntries = entries.stream().map( e -> {
                    var pair = new BsonArray();
                    pair.add(kCodec.encode(e.getKey(), extraInfo));
                    pair.add(vCodec.encode(e.getValue(), extraInfo));
                    return pair;
                }
        ).toList();
        return new BsonArray(bsonEntries);
    }

    @Override
    public @NonNull Schema toSchema(@NonNull SchemaContext schemaContext) {
        return new ArraySchema();
    }
}
