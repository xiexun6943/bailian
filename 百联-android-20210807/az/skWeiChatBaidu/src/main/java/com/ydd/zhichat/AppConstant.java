package com.ydd.zhichat;


public class AppConstant {
    /**
     * 某些地方选择数据使用的常量
     */
    public static final String EXTRA_ACTION = "action";// 进入这个类的执行的操作
    public static final int ACTION_NONE = 0;  // 不执行操作
    public static final int ACTION_SELECT = 1;// 执行选择操作
    public static final String EXTRA_SELECT_IDS = "select_ids";// 选择对应项目的ids
    public static final String EXTRA_SELECT_ID = "select_id";// 选择对应项目的id
    public static final String EXTRA_SELECT_NAME = "select_name";// 选择的对应项目的名称

    /**
     * 某些地方需要传递如ListView Position的数据
     */
    public static final String EXTRA_POSITION = "position";
    public static final int INVALID_POSITION = -1;

    // 用户信息参数，很多地方需要
    public static final String EXTRA_USER = "user";// user
    public static final String EXTRA_USER_ACCOUNT = "account";// account
    public static final String EXTRA_USER_ID = "userId";// userId
    public static final String EXTRA_NICK_NAME = "nickName";// nickName
    public static final String EXTRA_MESSAGE_ID = "messageId";
    public static final String EXTRA_IS_GROUP_CHAT = "isGroupChat";// 是否是群聊

    // BusinessCircleActivity需要的
    public static final String EXTRA_CIRCLE_TYPE = "circle_type";// 看的商务圈类型
    public static final int CIRCLE_TYPE_MY_BUSINESS = 0;// 看的商务圈类型,是我的商务圈
    public static final int CIRCLE_TYPE_PERSONAL_SPACE = 1;// 看的商务圈类型，是个人空间

    /**
     * 商务圈发布的常量
     */
    /* 发说说(图文) */
    public static final String EXTRA_IMAGES = "images";// 预览的那组图片
    public static final String EXTRA_CHANGE_SELECTED = "change_selected";// 是否可以改变选择，这样在ActivityResult中会回传重新选择的结果

    public static final String EXTRA_MSG_ID = "msg_id";// 公共消息id
    public static final String EXTRA_FILE_PATH = "file_path";// 语音、视频文件路径
    public static final String FILE_PAT_NAME = "file_name";//文件的名字
    public static final String EXTRA_IMAGE_FILE_PATH = "image_file_path";// 图片文件路径
    public static final String EXTRA_TIME_LEN = "time_len";// 语音、视频文件时长
    //位置经纬度
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";
    // 位置地图截图，
    public static final String EXTRA_SNAPSHOT = "snapshot";

    /* IM */
    public static final String EXTRA_FRIEND = "friend";

    /* 进入SingleImagePreviewActivity需要带上的参数 */
    public static final String EXTRA_IMAGE_URI = "image_uri";

    /* 进入ChatVideoPreviewActivity需要带上的参数 */
    public static final String EXTRA_VIDEO_FILE_URI = "video_file_url";
    public static final String EXTRA_VIDEO_FILE_PATH = "video_file_path";
    // 个人短视频预览带的特殊参数
    public static final String EXTRA_VIDEO_FILE_THUMB = "video_file_thumb";

    // 传出参数，选择的视频列表，
    public static final String EXTRA_VIDEO_LIST = "video_list";
    // 传入参数，是否支持多选，
    public static final String EXTRA_MULTI_SELECT = "multi_select";

    // 服务端集群需要的area参数
    public static final String EXTRA_CLUSTER_AREA = "cluster_area";

    public static final int PROCLAMATION = 0x1118;

    public static final int NOTICE_ID = 0x0817;

    // 银行类型
    public static final int ZHI_FU_BAO = 100;//支付宝
    public static final int ZHONG_GUO_YINHANG = 101;//中国银行
    public static final int ZHONG_GUO_JIANSHE_YINHANG = 102;//建行
    public static final int ZHONG_GUO_GONGSHANG_YINHANG = 103;//工行
    public static final int ZHONG_GUO_NONGYE_YINHANG = 104;//农行
    public static final int ZHONG_GUO_JIAOTONG_YINHANG = 105;//交通
    public static final int ZHONG_GUO_YOUZHENG_YINHANG = 106;//邮政

}
