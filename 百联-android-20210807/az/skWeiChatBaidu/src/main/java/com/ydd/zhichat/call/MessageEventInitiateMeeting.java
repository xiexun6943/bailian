package com.ydd.zhichat.call;

import java.util.List;

public class MessageEventInitiateMeeting {
    public final boolean isAudio;
    public final List<String> list;

    public MessageEventInitiateMeeting(boolean isAudio, List<String> list) {
        this.isAudio = isAudio;
        this.list = list;
    }
}
