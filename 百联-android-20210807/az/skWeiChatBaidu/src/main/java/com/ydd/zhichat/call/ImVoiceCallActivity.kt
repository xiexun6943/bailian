package com.ydd.zhichat.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.StringSignature
import com.cjt2325.cameralibrary.util.LogUtil
import com.ydd.zhichat.MyApplication
import com.ydd.zhichat.R
import com.ydd.zhichat.bean.Friend
import com.ydd.zhichat.bean.message.ChatMessage
import com.ydd.zhichat.bean.message.XmppMessage
import com.ydd.zhichat.bean.message.XmppMessage.TYPE_END_CONNECT_VOICE
import com.ydd.zhichat.bean.message.XmppMessage.TYPE_NO_CONNECT_VOICE
import com.ydd.zhichat.broadcast.MsgBroadcast
import com.ydd.zhichat.db.InternationalizationHelper
import com.ydd.zhichat.db.dao.ChatMessageDao
import com.ydd.zhichat.db.dao.FriendDao
import com.ydd.zhichat.db.dao.UserAvatarDao
import com.ydd.zhichat.helper.AvatarHelper
import com.ydd.zhichat.helper.CutoutHelper
import com.ydd.zhichat.ui.base.BaseActivity
import com.ydd.zhichat.util.*
import de.greenrobot.event.EventBus
import de.greenrobot.event.Subscribe
import de.greenrobot.event.ThreadMode
import io.agora.rtc.Constants
import io.agora.rtc.IAudioEffectManager
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import kotlinx.android.synthetic.main.activity_voice_call.*
import java.text.SimpleDateFormat
import java.util.*

class ImVoiceCallActivity : BaseActivity(), View.OnClickListener {

    //region declare variable
    private var mUid: String = "0"
    private var friendUid: String = "0"
    private var myName: String = ""
    private var firendName: String = ""
    private var channel: String = ""
    private var appId: String = ""
    private var myToken: String = ""
    private var callOrReceive: Int = CallManager.CALL
    private var isHangUpSelf: Boolean = true //是否是自己先挂断
    private var callingOften: Int = 0 //接通后通话用时
    private var mRtcEngine: RtcEngine? = null
    private var mAudioEffectManager: IAudioEffectManager? = null
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var startTime // 通话开始时间
            : Long = 0
    private var stopTime // 通话结束时间
            : Long = 0

    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    private val STOP_CALL_OFTEN = 30 * 1000//拨打但未接通用时等待时长
    //endregion

    companion object {
        private const val KEY_1 = "myUid"
        private const val KEY_2 = "friendUid"
        private const val KEY_3 = "myName"
        private const val KEY_4 = "channel"
        private const val KEY_5 = "appid"
        private const val KEY_6 = "token"
        private const val KEY_7 = "callOrReceive"
        private const val KEY_8 = "friendName"

        fun start(context: Context,
                  fromuserid: String,
                  touserid: String,
                  username: String,
                  firendName: String,
                  channel: String,
                  appid: String,
                  token: String,
                  callOrReceive: Int) {
            val starter = Intent(context, ImVoiceCallActivity::class.java)
            starter.putExtra(KEY_1, fromuserid)
            starter.putExtra(KEY_2, touserid)
            starter.putExtra(KEY_3, username)
            starter.putExtra(KEY_8, firendName)
            starter.putExtra(KEY_4, channel)
            starter.putExtra(KEY_5, appid)
            starter.putExtra(KEY_6, token)
            starter.putExtra(KEY_7, callOrReceive)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        CutoutHelper.setWindowOut(window)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)
        initView()
    }

    //region callback
    fun initView() {
        getParentData()
        AvatarHelper.getInstance().displayAvatar(friendUid, img_portrait, true)
        tv_name.text = firendName
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            intoCall()
        }
        EventBus.getDefault().register(this)
        setListener()
    }

    //对方取消
    @Subscribe(threadMode = ThreadMode.MainThread)
    fun helloEventBus(message: MessageHangUpPhone) { // 对方取消了 || 其他端 (接听 || 取消)了
        if (message.chatMessage.getFromUserId().equals(friendUid) || message.chatMessage.getFromUserId().equals(mUid)) {
            reallyFinish()
        }
    }

    //对方接听
    @Subscribe(threadMode = ThreadMode.MainThread)
    fun helloEventBus(message: MessageEventSipPreview) { // 对方接听
        talking()
    }

    private fun sendAnswerMessage() {
        val message = ChatMessage()
        message.type = XmppMessage.TYPE_CONNECT_VOICE
        message.content = ""
        message.fromUserId = mUid
        message.toUserId = friendUid
        message.packetId = UUID.randomUUID().toString().replace("-".toRegex(), "")
        message.fromUserName = myName
        message.timeSend = TimeUtils.sk_time_current_time()
        coreManager.sendChatMessage(friendUid, message)
    }

    private fun sendCallMessage() {
        if (callOrReceive == CallManager.CALL) {
            val message = ChatMessage()
            message.type = XmppMessage.TYPE_IS_CONNECT_VOICE
            message.content = InternationalizationHelper.getString("JXSip_invite") + " " + InternationalizationHelper.getString("JX_VoiceChat")
            message.fromUserId = mUid
            message.fromUserName = myName
            if (!TextUtils.isEmpty(channel)) {
                message.filePath = channel
            }
            message.packetId = UUID.randomUUID().toString().replace("-".toRegex(), "")
            message.timeSend = TimeUtils.sk_time_current_time()
            coreManager.sendChatMessage(friendUid, message)
            CallManager.showCallNotification(MyApplication.getInstance(),
                    mUid,
                    friendUid,
                    myName,
                    firendName,
                    channel,
                    appId,
                    myToken,
                    CallManager.CALL,
                    "等待对方接听",
                    null,
                    false)
        }
    }

    private fun sendHangUpMessage() {
        val message = ChatMessage()
        message.type = TYPE_NO_CONNECT_VOICE
        message.isMySend = true
        message.fromUserId = mUid
        message.fromUserName = myName
        message.toUserId = friendUid
        message.packetId = UUID.randomUUID().toString().replace("-".toRegex(), "")
        message.timeSend = TimeUtils.sk_time_current_time()
        if (ChatMessageDao.getInstance().saveNewSingleChatMessage(mUid, friendUid, message)) { // 更新聊天界面
            MsgBroadcast.broadcastMsgChatUpdate(this, message.packetId)
            // 更新朋友表
            FriendDao.getInstance().updateFriendContent(mUid, friendUid,
                    InternationalizationHelper.getString("JXSip_Canceled").toString() + " " + InternationalizationHelper.getString("JX_VoiceChat"),
                    TYPE_NO_CONNECT_VOICE, TimeUtils.sk_time_current_time())
        }
        coreManager.sendChatMessage(friendUid, message)
        MsgBroadcast.broadcastMsgUiUpdate(this) // 更新消息界面
    }


    override fun onDestroy() {
        if (!TextUtils.isEmpty(appId)) {
            mWindowManager?.let {
                mWindowManager!!.removeView(mFloatingLayout)
                mWindowManager = null
            }
            RtcEngine.destroy()
            mRtcEngine = null
            task.cancel()
            callingOften = 0
            ringStop()
        }
        CallManager.closeCallNotification(this)
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onPause() {
        //不是结束界面，不是手动最小化，即点击home键时启动悬浮窗
        if (!isRellyFinish && !isManual) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                if (FloatWindowUtil.checkFloatPermission(this@ImVoiceCallActivity)) {
                    startVoiceFloatWindow(true)
                } else {
                    ToastUtil.showToast(this@ImVoiceCallActivity, "悬浮框未开启")
                }
            } else {
                startVoiceFloatWindow(true)
            }
        }
        //手动最小化之后，将标志复原
        isManual = false
        super.onPause()
    }

    override fun onBackPressed() {
        ToastUtil.showToast(this@ImVoiceCallActivity, "正在通话中，请先停止通话")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        LogUtil.i("onRequestPermissionsResult " + grantResults[0] + " " + requestCode)
        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    intoCall()
                } else {
                    ToastUtil.showToast(this@ImVoiceCallActivity, "No permission for " + Manifest.permission.RECORD_AUDIO)
                    reallyFinish()
                }
            }
        }
    }


    override fun onClick(v: View?) {
        if (ClickFilter.isFastDoubleClick()) return
        when (v?.id) {
            R.id.img_answer -> {//接听
                if (NetworkUtils.isConnected()) {
                    joinCall()
                } else {
                    ToastUtil.showToast(this@ImVoiceCallActivity, "网络不可用")
                    refuseCall()
                }
            }
            R.id.img_refuse -> {//拒接
                refuseCall()
            }
            R.id.img_mute -> {//静音
                if (img_mute.isSelected) {
                    img_mute.isSelected = false
                    img_mute.setImageResource(R.drawable.ic__call_mute)
                } else {
                    img_mute.isSelected = true
                    img_mute.setImageResource(R.drawable.ic__call_mute_open)
                }
                mRtcEngine?.muteLocalAudioStream(img_mute.isSelected)
            }
            R.id.img_speaker -> {//免提
                if (img_speaker.isSelected) {
                    img_speaker.isSelected = false
                    img_speaker.setImageResource(R.drawable.ic_call_speaker)
                } else {
                    img_speaker.isSelected = true
                    img_speaker.setImageResource(R.drawable.ic_call_speaker_open)
                }
                mRtcEngine?.setEnableSpeakerphone(img_speaker.isSelected)
            }
            R.id.img_end_call -> {//挂断
                task.cancel()
                task2.cancel()
                stopTime = System.currentTimeMillis()
                if (JitsistateMachine.isIncall) {
                    stopCallInCalling()
                } else {
                    stopCallBeforCalling()
                }
                //自己离开
                if (mRtcEngine != null) {
                    mRtcEngine?.leaveChannel()
                }
                reallyFinish()
            }
            R.id.iv_float -> {//开启悬浮窗
                if (FloatWindowUtil.checkFloatPermission(this@ImVoiceCallActivity)) {
                    startVoiceFloatWindow()
                } else {
                    ToastUtil.showToast(this@ImVoiceCallActivity, "悬浮框未开启")
                }
            }
        }
    }

    //endregion

    //region init method

    private fun getParentData() {
        mUid = intent.getStringExtra(KEY_1)
        friendUid = intent.getStringExtra(KEY_2)
        myName = intent.getStringExtra(KEY_3)
        firendName = intent.getStringExtra(KEY_8)
        channel = intent.getStringExtra(KEY_4)
        appId = intent.getStringExtra(KEY_5)
        myToken = intent.getStringExtra(KEY_6)
        callOrReceive = intent.getIntExtra(KEY_7, CallManager.CALL)
    }

    /**
     * 设置按钮的点击事件
     */
    fun setListener() {
        img_answer.setOnClickListener(this)
        img_refuse.setOnClickListener(this)
        img_mute.setOnClickListener(this)
        img_speaker.setOnClickListener(this)
        img_end_call.setOnClickListener(this)
        iv_float.setOnClickListener(this)
    }

    /**
     * 展示朋友的信息
     */
    private fun bindView() {

    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() { // Tutorial Step 1
        //自己加入
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            Log.i("", "RtcEngine onJoinChannelSuccess~channel:" + channel + "~uid:" + uid)
            if (callOrReceive == CallManager.RECEIVE_CALL) {
                JitsistateMachine.isIncall = true
                talking()
                sendAnswerMessage()
            } else {
                //开始计时
                timing()
                sendCallMessage()
            }

        }

        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
            Log.i("", "RtcEngine onLeaveChannel~channel:" + channel)
            JitsistateMachine.isIncall = false

        }

        //对方加入
        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.i("RtcEngine onUserJoined", "~channel:" + channel + "~uid:" + uid)
            if (callOrReceive == CallManager.CALL) {
                JitsistateMachine.isIncall = true
                talking()
            }
        }

        //对方离开
        override fun onUserOffline(uid: Int, reason: Int) { // Tutorial Step 4
            Log.i("RtcEngine onUserOffline", "~channel:" + channel + "~uid:" + uid)
            JitsistateMachine.isIncall = false
            isHangUpSelf = false//对方先挂断
            mRtcEngine?.leaveChannel()
            if (uid == friendUid.toInt()) {
                if (!TextUtils.isEmpty(channel)) {
                    stopTime = System.currentTimeMillis()
                    task2.cancel()
                }
            }
        }

        //对方静音
        override fun onUserMuteAudio(uid: Int, muted: Boolean) { // Tutorial Step 6
            Log.i("RtcEngine onUserOffline", "~channel:" + channel + "~uid:" + uid)
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            ToastUtil.showToast(this@ImVoiceCallActivity, "网络不可用")
            mRtcEngine?.leaveChannel()
        }

    }


    //检查权限
    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        LogUtil.i("checkSelfPermission $permission $requestCode")
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    // 初始化
    private fun initializeAgoraEngine() {
        LogUtil.i("RtcEngine initializeAgoraEngine-  appId:$appId")
        try {
            mRtcEngine = RtcEngine.create(this, appId, mRtcEventHandler)
            mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            mAudioEffectManager = mRtcEngine?.audioEffectManager
            mRtcEngine?.setDefaultAudioRoutetoSpeakerphone(false)//默认听筒
        } catch (e: Exception) {
            throw RuntimeException("NEED TO check rtc sdk createFriendInfoManager fatal error\n" + Log.getStackTraceString(e))
        }
    }


    private fun intoCall() {
        if (TextUtils.isEmpty(appId)) {
            reallyFinish()
        } else {
            initializeAgoraEngine()
            //初始化
            if (callOrReceive == CallManager.CALL) {
                callOut()
                //加入电话
                joinChannel()
            } else if (callOrReceive == CallManager.RECEIVE_CALL) {
                callIn()
            }
        }
    }

    //endregion

    //region call status

    //自己发起通话 - 加入通话
    private fun joinChannel() {
        var code = mRtcEngine?.joinChannel(myToken, channel, "Extra Optional Data", mUid.toInt())
        Log.i("RtcEngine - ", "\nchannel:" + channel + "\nmyToken:" + myToken + "\nmUid:" + mUid + "\ncode:" + code)
    }

    //对方来电 - 接通来电
    private fun joinCall() {
        var code = mRtcEngine?.joinChannel(myToken, channel, "Extra Optional Data", mUid.toInt())
        Log.i("RtcEngine - ", "\nchannel:" + channel + "\nmyToken:" + myToken + "\nmUid:" + mUid + "\ncode:" + code)
    }

    // 拒接来电 - 未接通
    private fun refuseCall() {
        task.cancel()
        sendHangUpMessage()
        reallyFinish()
    }

    //接通前挂断
    private fun stopCallBeforCalling() {
        EventBus.getDefault().post(MessageEventCancelOrHangUp(TYPE_NO_CONNECT_VOICE, friendUid,
                InternationalizationHelper.getString("JXSip_Canceled").toString() + " " + InternationalizationHelper.getString("JX_VoiceChat"), 0))
        JitsistateMachine.isIncall = false
    }

    //接通后挂断
    private fun stopCallInCalling() {
        var time = (stopTime - startTime).toInt() / 1000
        EventBus.getDefault().post(MessageEventCancelOrHangUp(TYPE_END_CONNECT_VOICE, friendUid,
                InternationalizationHelper.getString("JXSip_Canceled").toString() + " " + InternationalizationHelper.getString("JX_VoiceChat"),
                time))
        JitsistateMachine.isIncall = false
    }

    //endregion

    //region sendMsg


    /**
     * 获取时间戳
     */
    fun getTimeShort(tiem: Long): String {
        val formatter = SimpleDateFormat("mm:ss")
        val currentTime = Date(tiem)
        return formatter.format(currentTime)
    }

    private var task2: TimerTask = object : TimerTask() {
        override fun run() {
            //刷新通话时间
            if (JitsistateMachine.isIncall) {
                runOnUiThread {
                    if (callingOften % 5 == 0) {
                        CallManager.showCallNotification(MyApplication.getInstance(),
                                mUid,
                                friendUid,
                                myName,
                                firendName,
                                channel,
                                appId,
                                myToken,
                                CallManager.CALL,
                                getTimeShort(callingOften.toLong() * 1000),
                                null,
                                false)
                    }
                    tv_calling_time.text = getTimeShort(callingOften.toLong() * 1000)
                    callingOften++
                }
            }
        }
    }

    //endregion

    //region timer

    private var timer = Timer()
    private var task: TimerTask = object : TimerTask() {
        override fun run() {
            //超时未接就挂段
            runOnUiThread {
                if (!JitsistateMachine.isIncall) {
                    mRtcEngine?.leaveChannel()
                    ToastUtil.showToast(this@ImVoiceCallActivity, "无人应答")
                }
            }
        }
    }


    //开始计时 - 定时停止
    private fun timing() {
        timer.schedule(task, STOP_CALL_OFTEN.toLong())
    }

    //endregion

    //region viewUI & ring

    // 正在拨出
    private fun callOut() {
        runOnUiThread {
            lin_receive.visibility = View.GONE
            lin_call.visibility = View.VISIBLE
            tv_tip.visibility = View.VISIBLE
            tv_calling_time.visibility = View.GONE
            tv_tip.text = "正在呼叫中..."
            ringPlay()
        }
    }

    //来电响铃中
    private fun callIn() {
        runOnUiThread {
            lin_receive.visibility = View.VISIBLE
            lin_call.visibility = View.GONE
            tv_tip.visibility = View.VISIBLE
            tv_calling_time.visibility = View.GONE
            tv_tip.text = "邀请你进行语音通话..."
            ringPlay()
            CallManager.showCallNotification(MyApplication.getInstance(),
                    mUid,
                    friendUid,
                    myName,
                    firendName,
                    channel,
                    appId,
                    myToken,
                    CallManager.RECEIVE_CALL,
                    "邀请你进行语音通话...",
                    null,
                    false)
        }
    }

    //正在通话中
    private fun talking() {
        runOnUiThread {
            timer.schedule(task2, 0, 1000)
            startTime = System.currentTimeMillis()
            ringStop()
            lin_receive.visibility = View.GONE
            lin_call.visibility = View.VISIBLE
            tv_tip.visibility = View.GONE
            tv_calling_time.visibility = View.VISIBLE
            task.cancel()
            iv_float.visibility = View.VISIBLE
        }
    }


    private fun ringPlay() {
        runOnUiThread {
            mediaPlayer = MediaPlayer.create(this@ImVoiceCallActivity, R.raw.call)
            mediaPlayer!!.isLooping = true
            mediaPlayer!!.start()
            //接电话才振动
            if (callOrReceive == CallManager.RECEIVE_CALL) {
                vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val patter = longArrayOf(1000, 100, 1000, 100, 1000, 100)
                vibrator?.vibrate(patter, 0)
            }
        }
    }

    private fun ringStop() {
        runOnUiThread {
            try {
                if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                }
                if (vibrator != null) {
                    vibrator?.cancel()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //endregion


    //region floatWindow

    private var mWindowManager: WindowManager? = null
    private lateinit var wmParams: WindowManager.LayoutParams
    private var mFloatingLayout: View? = null
    private lateinit var smallSizePreviewLayout: View
    private var remoteUid: Int = 0
    //是否真正的想要结束
    private var isRellyFinish = false
    //是否是手动最小化
    private var isManual = false

    private fun reallyFinish() {
        isRellyFinish = true
        finish()
    }


    /**
     * 开启显示悬浮框
     */
    private fun startVoiceFloatWindow(isAutoIntoBack: Boolean = false) {
        try {
            initWindow()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        ToastUtil.showToast(this, "如果未显示悬浮窗，请前往设置中开启悬浮窗权限")
        if (!isAutoIntoBack) {
            isManual = true
            moveTaskToBack(true)//最小化Activity
        }
    }

    /**
     * 设置悬浮框基本参数（位置、宽高等）
     */
    private fun initWindow() {
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wmParams = getParams()//设置好悬浮窗的参数
        // 悬浮窗默认显示以左上角为起始坐标
        wmParams.gravity = Gravity.LEFT or Gravity.TOP
        //悬浮窗的开始位置，因为设置的是从左上角开始，所以屏幕左上角是x=0;y=0
        wmParams.x = 70
        wmParams.y = 210
        wmParams.format = PixelFormat.TRANSLUCENT;
        //得到容器，通过这个inflater来获得悬浮窗控件
        // 获取浮动窗口视图所在布局
        mFloatingLayout = LayoutInflater.from(this).inflate(R.layout.alert_float_voice_layout, null)
        smallSizePreviewLayout = mFloatingLayout?.findViewById(R.id.small_size_preview)!!
        //悬浮框点击事件
        smallSizePreviewLayout.setOnClickListener {
            //在这里实现点击重新回到Activity
            MyApplication.getInstance().startActivity(Intent(MyApplication.getInstance(), ImVoiceCallActivity::class.java))
        }
        //悬浮框触摸事件，设置悬浮框可拖动
        smallSizePreviewLayout.setOnTouchListener(FloatingListener())
        // 添加悬浮窗的视图
        mWindowManager!!.addView(mFloatingLayout, wmParams)
    }

    private fun getParams(): WindowManager.LayoutParams {
        wmParams = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            wmParams.type = WindowManager.LayoutParams.TYPE_PHONE
        } else {
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
        //设置可以显示在状态栏上
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        return wmParams
    }

    //开始触控的坐标，移动时的坐标（相对于屏幕左上角的坐标）
    private var mTouchStartX: Int = 0
    private var mTouchStartY: Int = 0
    private var mTouchCurrentX: Int = 0
    private var mTouchCurrentY: Int = 0
    //开始时的坐标和结束时的坐标（相对于自身控件的坐标）
    private var mStartX: Int = 0
    private var mStartY: Int = 0
    private var mStopX: Int = 0
    private var mStopY: Int = 0
    //判断悬浮窗口是否移动，这里做个标记，防止移动后松手触发了点击事件
    private var isMove: Boolean = false

    private inner class FloatingListener : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isMove = false
                    mTouchStartX = event.rawX.toInt()
                    mTouchStartY = event.rawY.toInt()
                    mStartX = event.x.toInt()
                    mStartY = event.y.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    mTouchCurrentX = event.rawX.toInt()
                    mTouchCurrentY = event.rawY.toInt()
                    wmParams.x += mTouchCurrentX - mTouchStartX
                    wmParams.y += mTouchCurrentY - mTouchStartY
                    mWindowManager!!.updateViewLayout(mFloatingLayout, wmParams)
                    mTouchStartX = mTouchCurrentX
                    mTouchStartY = mTouchCurrentY
                }
                MotionEvent.ACTION_UP -> {
                    mStopX = event.x.toInt()
                    mStopY = event.y.toInt()
                    if (Math.abs(mStartX - mStopX) >= 1 || Math.abs(mStartY - mStopY) >= 1) {
                        isMove = true
                    }
                }
            }
            //如果是移动事件不触发OnClick事件，防止移动的时候一放手形成点击事件
            return isMove
        }
    }

    override fun onRestart() {
        super.onRestart()
        //不显示悬浮框
        if (mWindowManager != null && mFloatingLayout != null) {
            mWindowManager!!.removeView(mFloatingLayout)
            mWindowManager = null
        }

    }

    /**
     * 显示头像
     *
     * @param userId
     * @param imageView
     * @param isThumb
     */
    fun displayAvatar(userId: String, imageView: ImageView, isThumb: Boolean) {
        Log.e("xuan", "displayAvatar: $userId")
        if (userId == Friend.ID_SYSTEM_MESSAGE) {
            imageView.setImageResource(R.drawable.im_notice)
            return
        } else if (userId == Friend.ID_NEW_FRIEND_MESSAGE) {
            imageView.setImageResource(R.drawable.im_new_friends)
            return
        } else if (userId == "android" || userId == "ios") { // 我的手机
            imageView.setImageResource(R.drawable.fdy)
            return
        } else if (userId == "pc" || userId == "mac" || userId == "web") { // 我的电脑
            imageView.setImageResource(R.drawable.feb)
            return
        }
        val url: String = AvatarHelper.getAvatarUrl(userId, isThumb)
        if (TextUtils.isEmpty(url)) {
            return
        }
        val time: String = UserAvatarDao.getInstance().getUpdateTime(userId)
        Glide.with(MyApplication.getContext())
                .load(url)
                .placeholder(R.drawable.avatar_normal)
                .signature(StringSignature(time))
                .dontAnimate()
                .error(R.drawable.avatar_normal)
                .into(imageView)
    }

    //endregion
}