package com.ydd.zhichat.view.chatHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ydd.zhichat.R;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.bean.message.XmppMessage;
import com.ydd.zhichat.db.InternationalizationHelper;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;


class CallViewHolder extends AChatHolderInterface {

    ImageView ivTextImage;
    TextView mTvContent;

    @Override
    public int itemLayoutId(boolean isMysend) {
        return isMysend ? R.layout.chat_from_item_call : R.layout.chat_to_item_call;
    }

    @Override
    public void initView(View view) {
        ivTextImage = view.findViewById(R.id.chat_text_img);
        mTvContent = view.findViewById(R.id.chat_text);
        mRootView = view.findViewById(R.id.chat_warp_view);
    }

    @Override
    public void fillData(ChatMessage message) {
        switch (message.getType()) {
            case XmppMessage.TYPE_NO_CONNECT_VOICE: {
                String content;
                if (message.getTimeLen() == 0) {
                    content = InternationalizationHelper.getString("JXSip_Canceled") + InternationalizationHelper.getString("JX_VoiceChat");
                } else {
                    content = InternationalizationHelper.getString("JXSip_noanswer");
                }
                mTvContent.setText(content);
                ivTextImage.setImageResource(R.drawable.ic_chat_no_conn);
            }
            break;
            case XmppMessage.TYPE_END_CONNECT_VOICE: {
                // 结束
                int timeLen = message.getTimeLen();
                mTvContent.setText(InternationalizationHelper.getString("JXSip_finished") + InternationalizationHelper.getString("JX_VoiceChat") + ","
                        + InternationalizationHelper.getString("JXSip_timeLenth") + ":" + getTimeLengthString(timeLen));
                ivTextImage.setImageResource(R.drawable.ic_chat_no_conn);
            }
            break;

            case XmppMessage.TYPE_NO_CONNECT_VIDEO: {
                String content;
                if (message.getTimeLen() == 0) {
                    content = InternationalizationHelper.getString("JXSip_Canceled") + InternationalizationHelper.getString("JX_VideoChat");
                } else {
                    content = InternationalizationHelper.getString("JXSip_noanswer");
                }

                mTvContent.setText(content);
                ivTextImage.setImageResource(R.drawable.ic_chat_end_conn);
            }
            break;
            case XmppMessage.TYPE_END_CONNECT_VIDEO: {
                // 结束
                int timeLen = message.getTimeLen();
                mTvContent.setText(InternationalizationHelper.getString("JXSip_finished") + InternationalizationHelper.getString("JX_VideoChat") + ","
                        + InternationalizationHelper.getString("JXSip_timeLenth") + ":" + getTimeLengthString(timeLen));
                ivTextImage.setImageResource(R.drawable.ic_chat_end_conn);
            }

            break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VOICE: {
                mTvContent.setText(R.string.tip_invite_voice_meeting);
                ivTextImage.setImageResource(R.drawable.ic_chat_no_conn);
            }
            break;
            case XmppMessage.TYPE_IS_MU_CONNECT_VIDEO: {
                mTvContent.setText(R.string.tip_invite_video_meeting);
                ivTextImage.setImageResource(R.drawable.ic_chat_end_conn);
            }
            break;
            case XmppMessage.TYPE_IS_MU_CONNECT_TALK: {
                mTvContent.setText(R.string.tip_invite_talk_meeting);
                ivTextImage.setImageResource(R.drawable.ic_chat_end_conn);
            }
            break;

        }
    }

    @NotNull
    private String getTimeLengthString(int timeLen) {
        long hour = TimeUnit.SECONDS.toHours(timeLen);
        long minute = TimeUnit.SECONDS.toMinutes(timeLen % TimeUnit.HOURS.toSeconds(1));
        long second = timeLen % TimeUnit.MINUTES.toSeconds(1);
        StringBuilder sb = new StringBuilder();
        if (hour > 0) {
            sb.append(hour).append(mContext.getString(R.string.hour));
        }
        if (minute > 0) {
            sb.append(minute).append(mContext.getString(R.string.minute));
        }
        sb.append(second).append(mContext.getString(R.string.second));
        return sb.toString();
    }

    @Override
    protected void onRootClick(View v) {

    }


    /**
     * 重写该方法，return true 表示自动发送已读
     *
     * @return
     */
    @Override
    public boolean enableSendRead() {
        return true;
    }
}
