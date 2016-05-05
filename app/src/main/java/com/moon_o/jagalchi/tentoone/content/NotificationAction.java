package com.moon_o.jagalchi.tentoone.content;

/**
 * Created by mucha on 16. 4. 21.
 */
public enum NotificationAction {
    GALLERY_ACTION("com.moon_o.tentoone.tentoone.captureservice.gallery"),
    START_ACTION("com.moon_o.tentoone.tentoone.captureservice.start"),
    STOP_ACTION("com.moon_o.tentoone.tentoone.captureservice.stop"),
    RESET_ACTION("com.moon_o.tentoone.tentoone.captureservice.reset"),
    RESET_NOT_MESSAGE_ACTION("com.moon_o.tentoone.tentoone.captureservice.notmessagereset"),
    SAVE_ACTION("com.moon_o.tentoone.tentoone.captureservice.save"),
    EXCEPTION_ACTION("com.moon_o.tentoone.tentoone.captureservice.exception"),
    LIMIT_ACTION("com.moon_o.tentoone.tentoone.captureservice.limit")
    ;

    private final String action;

    NotificationAction(String action) {
        this.action = action;
    }

    public String getString() {
        return this.action;
    }
}
