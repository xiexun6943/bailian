package com.ydd.zhichat.ui.backup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.example.qrcode.utils.CommonUtils;
import com.ydd.zhichat.R;
import com.ydd.zhichat.Reporter;
import com.ydd.zhichat.bean.EventSentChatHistory;
import com.ydd.zhichat.bean.message.ChatMessage;
import com.ydd.zhichat.db.dao.ChatMessageDao;
import com.ydd.zhichat.helper.DialogHelper;
import com.ydd.zhichat.ui.base.BaseActivity;
import com.ydd.zhichat.util.AsyncUtils;
import com.ydd.zhichat.util.ToastUtil;
import com.ydd.zhichat.view.TipDialog;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.List;

import de.greenrobot.event.EventBus;
import okhttp3.HttpUrl;

public class SendChatHistoryActivity extends BaseActivity {
    public static boolean flag;
    private List<String> selectedUserIdList;
    @Nullable
    private ServerSocket serverSocket;

    public static void start(Context ctx, Collection<String> userIdList) {
        Intent intent = new Intent(ctx, SendChatHistoryActivity.class);
        intent.putExtra("userIdList", JSON.toJSONString(userIdList));
        ctx.startActivity(intent);
    }

    public static void moveToFront(Context ctx) {
        if (!flag) {
            return;
        }
        Intent intent = new Intent(ctx, SendChatHistoryActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_chat_history);

        flag = true;

        initActionBar();

        selectedUserIdList = JSON.parseArray(getIntent().getStringExtra("userIdList"),
                String.class);

        Log.i(TAG, "onCreate: list = " + selectedUserIdList);

        AsyncUtils.doAsync(this, t -> {
            String message = getString(R.string.tip_migrate_chat_history_failed);
            if (!this.isFinishing()) {
                Reporter.post(message, t);
                runOnUiThread(() -> {
                    TipDialog dialog = new TipDialog(this);
                    dialog.setmConfirmOnClickListener(getString(R.string.tip_migrate_chat_history_failed), () -> {
                        finish();
                    });
                    dialog.show();
                });
            } else {
                // ????????????????????????????????????socket,
                Log.w(TAG, message, t);
            }
        }, c -> {
            String ip = getIPAddress();
            serverSocket = new ServerSocket(0, 1, InetAddress.getByName(ip));
            try (ServerSocket server = serverSocket) {
                Log.i(TAG, "bind: " + server);
                c.uiThread(r -> {
                    showQrCode(ip, server.getLocalPort());
                });
                try (Socket accept = server.accept()) {
                    c.uiThread(r -> {
                        DialogHelper.showMessageProgressDialog(this, getString(R.string.tip_migrate_chat_history_sending));
                    });
                    try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(accept.getOutputStream()))) {
                        String ownerId = coreManager.getSelf().getUserId();
                        for (String friendId : selectedUserIdList) {
                            Log.i(TAG, "write: ownerId=" + ownerId + ", friendId=" + friendId);
                            write(output, ownerId + "," + friendId);
                            ChatMessageDao.getInstance().exportChatHistory(coreManager.getSelf().getUserId(), friendId, chatMessageIterator -> {
                                while (chatMessageIterator.hasNext()) {
                                    ChatMessage chatMessage = chatMessageIterator.next();
                                    Log.i(TAG, "output chatMessage, fromUserName: " + chatMessage.getFromUserName() + ", content: " + chatMessage.getContent());
                                    write(output, chatMessage.toJsonString());
                                }
                            });
                        }
                        output.flush();
                    }
                }
            }
            c.uiThread(r -> {
                DialogHelper.dismissProgressDialog();
                EventBus.getDefault().post(new EventSentChatHistory());
                ToastUtil.showToast(this, R.string.tip_send_chat_history_success);
                finish();
            });
        });
    }

    private void write(DataOutputStream output, String str) throws IOException {
        byte[] bytes = str.getBytes();
        // ???????????????????????????
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    @Override
    protected void onDestroy() {
        flag = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "onDestroy: serverSocket????????????", e);
            }
        }
        super.onDestroy();
    }

    @NonNull
    @SuppressWarnings("deprecation")
    public String getIPAddress() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    private void showQrCode(String ip, int localPort) {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        // ???????????????


        String str = HttpUrl.parse(coreManager.getConfig().website)
                .newBuilder()
                .addQueryParameter("action", ReceiveChatHistoryActivity.QR_CODE_ACTION_SEND_CHAT_HISTORY)
                .addQueryParameter("ip", ip)
                .addQueryParameter("port", String.valueOf(localPort))
                .addQueryParameter("userId", coreManager.getSelf().getUserId())
                .build()
                .toString();
        Bitmap bitmap = CommonUtils.createQRCode(str, screenWidth - 200, screenWidth - 200);

        ImageView ivQrCode = findViewById(R.id.ivQrCode);
        ivQrCode.setImageBitmap(bitmap);
    }

    private void initActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        findViewById(R.id.iv_title_left).setOnClickListener((v) -> {
            onBackPressed();
        });
        TextView tvTitle = findViewById(R.id.tv_title_center);
        tvTitle.setText(getString(R.string.send_chat_history));
    }
}
