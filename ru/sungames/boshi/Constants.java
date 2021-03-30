package ru.sungames.boshi;

public class onstants {
    public static final int[] VIEWPORT_SIZE = new int[] {1280, 720};
    public static final int[] GAME_VIEWPORT_SIZE = new int[] {20, 11};

    public enum Maps {
        FIRST("maps/first/first.tmx");

        public final String path;
        Maps(String path) {
            this.path = path;
        }
    }

    public enum Textures {
        LONG_BUTTON_DEFAULT("interfaces/button/button_long_default"),
        LONG_BUTTON_DRAGGED("interfaces/button/button_long_dragged"),
        LONG_BUTTON_SELECTED("interfaces/button/button_long_selected"),

        SHORT_BUTTON_DEFAULT("interfaces/button/button_short_default"),
        SHORT_BUTTON_DRAGGED("interfaces/button/button_short_dragged"),
        SHORT_BUTTON_SELECTED("interfaces/button/button_short_selected");

        public final String key;
        Textures(String key) {
            this.key = key;
        }
    }
}
