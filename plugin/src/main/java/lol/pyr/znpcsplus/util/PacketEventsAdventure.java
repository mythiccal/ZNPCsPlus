package lol.pyr.znpcsplus.util;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * PacketEvents and this plugin load different Adventure copies, because this jar relocates Kyori.
 * Components have to be rebuilt with PacketEvents' serializer before they are written to a packet.
 */
public final class PacketEventsAdventure {
    private static final Object SERIALIZER = serializer();
    private static final Method FROM_JSON = method(SERIALIZER, "fromJson", String.class);
    private static final Class<?> COMPONENT_TYPE = FROM_JSON.getReturnType();
    private static final Method AS_JSON = method(SERIALIZER, "asJson", COMPONENT_TYPE);
    private static final Constructor<?> TEAM_INFO = teamInfoConstructor();
    private static final Class<?> NAMED_TEXT_COLOR = TEAM_INFO.getParameterTypes()[5];
    private static final Object WHITE = field(NAMED_TEXT_COLOR, "WHITE");
    private static final Object COLOR_NAMES = field(NAMED_TEXT_COLOR, "NAMES");
    private static final Method COLOR_VALUE = colorValue(COLOR_NAMES);
    private static final Constructor<?> LEGACY_PLAYER_DATA = legacyPlayerDataConstructor();
    private static final Constructor<?> PLAYER_INFO = playerInfoConstructor();

    private PacketEventsAdventure() {}

    public static Object component(Component component) {
        if (component == null) return null;
        String json = GsonComponentSerializer.gson().serialize(component);
        return invoke(FROM_JSON, SERIALIZER, json);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static EntityData<?> optionalComponent(int index, Component component) {
        Optional value = component == null ? Optional.empty() : Optional.of(component(component));
        return new EntityData(index, EntityDataTypes.OPTIONAL_ADV_COMPONENT, value);
    }

    public static String json(Component component) {
        return (String) invoke(AS_JSON, SERIALIZER, component(component));
    }

    public static WrapperPlayServerTeams.ScoreBoardTeamInfo team(Component displayName, Component prefix, Component suffix,
                                                                  WrapperPlayServerTeams.NameTagVisibility tagVisibility,
                                                                  WrapperPlayServerTeams.CollisionRule collisionRule,
                                                                  String colorName,
                                                                  WrapperPlayServerTeams.OptionData optionData) {
        Object color = colorName == null ? WHITE : invoke(COLOR_VALUE, COLOR_NAMES, colorName);
        return (WrapperPlayServerTeams.ScoreBoardTeamInfo) construct(TEAM_INFO,
                component(displayName), component(prefix), component(suffix),
                tagVisibility, collisionRule, color, optionData);
    }

    public static WrapperPlayServerPlayerInfo.PlayerData legacyPlayerData(Component displayName, UserProfile profile, GameMode gameMode, int latency) {
        return (WrapperPlayServerPlayerInfo.PlayerData) construct(LEGACY_PLAYER_DATA, component(displayName), profile, gameMode, latency);
    }

    public static WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo(UserProfile profile, boolean listed, int latency, GameMode gameMode, Component displayName) {
        Object[] args = new Object[PLAYER_INFO.getParameterCount()];
        args[0] = profile;
        args[1] = listed;
        args[2] = latency;
        args[3] = gameMode;
        args[4] = component(displayName);
        if (args.length > 6) args[6] = 0;
        if (args.length > 7) args[7] = false;
        return (WrapperPlayServerPlayerInfoUpdate.PlayerInfo) construct(PLAYER_INFO, args);
    }

    private static Object serializer() {
        try {
            Class<?> type = Class.forName("com.github.retrooper.packetevents.util.adventure.AdventureSerializer");
            return type.getMethod("serializer").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("packetevents adventure serializer is unavailable", exception);
        }
    }

    private static Constructor<?> teamInfoConstructor() {
        try {
            Class<?> type = Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams$ScoreBoardTeamInfo");
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 7 && COMPONENT_TYPE.isAssignableFrom(params[0]) && COMPONENT_TYPE.isAssignableFrom(params[1])) {
                    return constructor;
                }
            }
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("packetevents team info is unavailable", exception);
        }
        throw new IllegalStateException("packetevents team info constructor is unavailable");
    }

    private static Constructor<?> legacyPlayerDataConstructor() {
        try {
            Class<?> type = Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo$PlayerData");
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length == 4 && COMPONENT_TYPE.isAssignableFrom(params[0]) && params[3] == int.class) return constructor;
            }
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("packetevents player data is unavailable", exception);
        }
        throw new IllegalStateException("packetevents player data constructor is unavailable");
    }

    private static Constructor<?> playerInfoConstructor() {
        try {
            Class<?> type = Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate$PlayerInfo");
            Constructor<?> shortest = null;
            for (Constructor<?> constructor : type.getConstructors()) {
                Class<?>[] params = constructor.getParameterTypes();
                if (params.length < 6 || params[1] != boolean.class || params[2] != int.class) continue;
                if (!COMPONENT_TYPE.isAssignableFrom(params[4])) continue;
                if (shortest == null || params.length < shortest.getParameterCount()) shortest = constructor;
            }
            if (shortest != null) return shortest;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("packetevents player info is unavailable", exception);
        }
        throw new IllegalStateException("packetevents player info constructor is unavailable");
    }

    private static Method method(Object target, String name, Class<?>... params) {
        try {
            return target.getClass().getMethod(name, params);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("missing packetevents method " + name, exception);
        }
    }

    private static Object field(Class<?> type, String name) {
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("missing packetevents field " + name, exception);
        }
    }

    private static Method colorValue(Object names) {
        Method fallback = null;
        for (Method method : names.getClass().getMethods()) {
            if (!method.getName().equals("value") || method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0] == String.class) return method;
            fallback = method;
        }
        if (fallback != null) return fallback;
        throw new IllegalStateException("missing named text color lookup");
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("packetevents adventure call failed", exception);
        }
    }

    private static Object construct(Constructor<?> constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("packetevents adventure call failed", exception);
        }
    }
}
