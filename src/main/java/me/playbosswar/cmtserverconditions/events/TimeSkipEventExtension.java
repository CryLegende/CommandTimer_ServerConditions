package me.playbosswar.cmtserverconditions.events;

import me.playbosswar.com.CommandTimerPlugin;
import me.playbosswar.com.api.ConditionExtension;
import me.playbosswar.com.api.NeededValue;
import me.playbosswar.com.api.events.EventExtension;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class TimeSkipEventExtension extends EventExtension implements Listener {
    private static final String DEFAULT_SKIP_REASON = "NIGHT_SKIP";
    private static final String UNKNOWN_SKIP_REASON = "UNKNOWN";

    // TimeSkipEvent#getSkipReason() returns TimeSkipEvent.SkipReason up to 1.21 and ClockTimeSkipEvent.SkipReason since
    // 26.3 (TimeSkipEvent now extends ClockTimeSkipEvent). The method name is the same but the return type is part of
    // the method descriptor, so a direct call compiled against one API throws NoSuchMethodError on the other.
    // It is looked up by name instead, which works on both.
    private static final Method GET_SKIP_REASON = findSkipReasonGetter();

    private final ConditionExtension extension;

    public TimeSkipEventExtension(ConditionExtension extension) {
        this.extension = extension;
    }

    @Override
    public @NotNull String getEventName() {
        return "TIME_SKIP";
    }

    @Override
    public @NotNull String[] getEventDescription() {
        return new String[]{"Triggered when server time gets skipped, like when all players sleep"};
    }

    @Override
    public ArrayList<NeededValue<?>> getReturnedValues() {
        ArrayList<NeededValue<?>> values = new ArrayList<>();
        values.add(new NeededValue<>("WORLD_NAME", "Name of the world where the time gets skipped", String.class, ""));
        values.add(new NeededValue<>("SKIP_REASON", "Reason for the time skip, can be COMMAND, CUSTOM or NIGHT_SKIP", String.class, DEFAULT_SKIP_REASON));

        return values;
    }

    @EventHandler
    public void onSkip(TimeSkipEvent e) {
        World w = e.getWorld();
        String reason = getSkipReason(e);

        ArrayList<NeededValue<?>> values = new ArrayList<>();
        values.add(new NeededValue<>("WORLD_NAME", "Name of the world where the time gets skipped", String.class, w.getName()));
        values.add(new NeededValue<>("SKIP_REASON", "Reason for the time skip, can be COMMAND, CUSTOM or NIGHT_SKIP", String.class, reason));
        CommandTimerPlugin.getInstance().getEventsManager().handleTriggeredEvent(extension, this, values);
    }

    private static Method findSkipReasonGetter() {
        try {
            return TimeSkipEvent.class.getMethod("getSkipReason");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static String getSkipReason(TimeSkipEvent e) {
        if (GET_SKIP_REASON == null) {
            return UNKNOWN_SKIP_REASON;
        }

        try {
            Object reason = GET_SKIP_REASON.invoke(e);
            return reason != null ? reason.toString() : UNKNOWN_SKIP_REASON;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return UNKNOWN_SKIP_REASON;
        }
    }
}
