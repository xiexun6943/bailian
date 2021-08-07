package com.ydd.zhichat.call;


import com.ydd.zhichat.bean.message.ChatMessage;

/**
 * Created by Administrator on 2017/6/26 0026.
 */
public class MessageEventSipEVent {
    public final int number;
    public final String touserid;
    public ChatMessage message;

    public MessageEventSipEVent(int number, String touserid, ChatMessage message) {
        this.number = number;
        this.touserid = touserid;
        this.message = message;
    }
}