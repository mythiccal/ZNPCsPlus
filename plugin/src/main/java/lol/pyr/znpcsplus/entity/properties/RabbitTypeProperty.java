package lol.pyr.znpcsplus.entity.properties;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.PacketEntity;
import lol.pyr.znpcsplus.util.PacketEventsAdventure;
import lol.pyr.znpcsplus.util.RabbitType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Map;

public class RabbitTypeProperty extends EntityPropertyImpl<RabbitType> {
    private final int index;
    private final boolean legacyBooleans;
    private final boolean optional;
    private final Object serialized;

    public RabbitTypeProperty(int index, boolean legacyBooleans, boolean legacyNames, boolean optional) {
        super("rabbit_type", RabbitType.BROWN, RabbitType.class);
        this.index = index;
        this.legacyBooleans = legacyBooleans;
        this.optional = optional;
        Component name = Component.text("Toast");
        this.serialized = legacyNames || !optional ? LegacyComponentSerializer.legacySection().serialize(name) : name;
    }

    @Override
    public void apply(Player player, PacketEntity entity, boolean isSpawned, Map<Integer, EntityData<?>> properties) {
        RabbitType rabbitType = entity.getProperty(this);
        if (rabbitType == null) return;
        if (!rabbitType.equals(RabbitType.TOAST)) {
            properties.put(index, legacyBooleans ?
                    newEntityData(index, EntityDataTypes.BYTE, (byte) rabbitType.getId()) :
                    newEntityData(index, EntityDataTypes.INT, rabbitType.getId()));
            if (optional) {
                properties.put(2, PacketEventsAdventure.optionalComponent(2, null));
            } else {
                properties.put(2, new EntityData<>(2, EntityDataTypes.STRING, ""));
            }
        } else {
            if (optional) {
                properties.put(2, PacketEventsAdventure.optionalComponent(2, (Component) serialized));
            } else {
                properties.put(2, newEntityData(2, EntityDataTypes.STRING, (String) serialized));
            }
        }
    }
}
