package gg.crystalized.lobby;

/**
 * Shared hotbar slot numbers. Keeping these in one place prevents the lobby,
 * parkour, and game-server items from silently drifting into each other.
 */
public final class HotbarSlots {
    private HotbarSlots() {
    }

    public static final int NAVIGATOR = 0;
    public static final int FRIENDS = 1;
    public static final int QUEST = 2;
    public static final int SHARDCORE = 4;

    // Lobby parkour slots.
    public static final int PARKOUR_CHECKPOINT = 5;
    public static final int PARKOUR_RESTART = 6;
    public static final int PARKOUR_END = 7;

    // Lobby rank ability slots.
    public static final int SUN_ABILITY = 6;
    public static final int SUN_FLIGHT = 7;
    public static final int MOON_ABILITY = 7;

    // Game-server slots. Slot 8 is reserved for Unqueue while queued.
    public static final int REQUEUE = 7;
    public static final int BACK_TO_HUB = 5;
    public static final int UNQUEUE = 8;
}
