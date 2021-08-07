package com.ydd.zhichat.call;

import java.util.List;

/**
 * 邀请 加入会议
 */
public class MessageEventMeetingInvite {
    public final String roomid;
    public final String callnumber;
    public final String filename;
    public final String fromuserid;
    public final String fromusername;
    public final String objectId;
    public final List<String> meetinglist;
    public final Boolean isaudio;

    public MessageEventMeetingInvite(String roomid, String callnumber, String filename, String fromuserid, String fromusername, String objectId, List<String> meetinglist, boolean isaudio) {
        this.roomid = roomid;
        this.callnumber = callnumber;
        this.filename = filename;
        this.fromuserid = fromuserid;
        this.fromusername = fromusername;
        this.objectId = objectId;
        this.meetinglist = meetinglist;
        this.isaudio= isaudio;
    }
}