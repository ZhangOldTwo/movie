package com.hw.movie.event;

public class MessageEvent {
    private String message;

    public MessageEvent(String message) {
        this.message = message;
    }

    public boolean getMessage() {
        if ("开启快乐模式".equals(message)) {
            return true;
        } else if ("关闭快乐模式".equals(message)) {
            return false;
        }
        return false;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
