package lol.pyr.znpcsplus.entity.properties;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.PacketEntity;
import lol.pyr.znpcsplus.util.PacketEventsAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Map;

public class DinnerboneProperty extends EntityPropertyImpl<Boolean> {
    private final boolean optional;
    private final Object serialized;

    public DinnerboneProperty(boolean legacy, boolean optional) {
        super("dinnerbone", false, Boolean.class);
        this.optional = optional;
        Component name = Component.text("Dinnerbone");
        this.serialized = legacy || !optional ? LegacyComponentSerializer.legacySection().serialize(name) : name;
    }

    @Override
    public void apply(Player player, PacketEntity entity, boolean isSpawned, Map<Integer, EntityData<?>> properties) {
        if (optional) {
            properties.put(2, PacketEventsAdventure.optionalComponent(2, entity.getProperty(this) ? (Component) serialized : null));
        } else {
            properties.put(2, new EntityData<>(2, EntityDataTypes.STRING, entity.getProperty(this) ? (String) serialized : ""));
        }
    }
}
