package com.moon_o.jagalchi.jagalchi.content;

/**
 * Created by mucha on 16. 4. 21.
 */
public enum NotificationAction {
    MAIN_ACTION("com.moon_o.jagalchi.jagalchi.captureservice.main"),
    START_ACTION("com.moon_o.jagalchi.jagalchi.captureservice.start"),
    STOP_ACTION("com.moon_o.jagalchi.jagalchi.captureservice.stop"),
    RESET_ACTION("com.moon_o.jagalchi.jagalchi.captureservice.reset"),
    SAVE_ACTION("com.moon_o.jagalchi.jagalchi.captureservice.save"),
    SHARE_ACTION("com.moon_o.jagalchi.jagalchi.captureservice.share")
    ;

    private final String action;

    NotificationAction(String action) {
        this.action = action;
    }

    public String getString() {
        return this.action;
    }
}
