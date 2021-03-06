package com.ydd.zhichat.bean;

import com.ydd.zhichat.bean.message.ChatMessage;

public class EventNewNotice {
    private String text;
    private String roomJid;

    public EventNewNotice(ChatMessage chatMessage) {
        this.text = chatMessage.getContent();
        this.roomJid = chatMessage.getObjectId();
    }

    public String getText() {
        return text;
    }

    public String getRoomJid() {
        return roomJid;
    }
}
