package cn.xyz.mianshi.vo;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.alibaba.fastjson.JSON;

import lombok.Getter;
import lombok.Setter;
/**
 * 
 * @Description: TODO(客户端配置)
 * @author Administrator
 * @date 2018年9月20日 上午10:50:51
 * @version V1.0
 */
@Setter
@Getter
@Entity(value="clientConfig",noClassnameStored=true)
public class ClientConfig {
	@Id
	private long id = 10000;
	
	public String XMPPDomain;// xmpp虚拟域名
	
	public String XMPPHost;// xmpp主机host
	
	private String apiUrl;// 接口URL
	
	private String downloadAvatarUrl;// 头像访问URL
	
	private String downloadUrl;// 资源访问URL
	
	private String uploadUrl;// 资源上传URL
	
	private String liveUrl;// 直播URL
	
	private String jitsiServer;// 视频服务器URL
	
	private int isOpenRegister = 1; //是否开启注册
	
	private String address;// 请求config后得到用户IP，查询出地址
	/**
	 * 显示通讯录好友
	 * 0 不显示   1 显示
	 */
	private int  showContactsUser=1;
	
	private byte isOpenReadReceipt=1;//是否启用 已读消息回执
	
	private int displayRedPacket=1;// 是否开启ios红包
	
	private int hideSearchByFriends=1;// 是否隐藏好友搜索功能 0:隐藏 1：开启
	
	private String appleId;// IOS AppleId

	private String companyName;// 公司名称
	
	private String copyright;// 版权信息

	private String website;// 公司下载页网址
	
	private String headBackgroundImg;// 头部导航背景图
	
	private int isCommonFindFriends = 0;// 普通用户是否能搜索好友 0:允许 1：不允许

	private int isCommonCreateGroup = 0;// 普通用户是否能建群 0:允许 1：不允许
	
	private int isOpenPositionService = 0;// 是否开启位置相关服务 0：开启 1：关闭 
	
	private byte isOpenAPNSorJPUSH = 1;// IOS推送平台 0：APNS  1：极光推送开发版  2：极光推送生产版

	private byte isOpenRoomSearch = 0;// 是否开启群组搜索 0：开启 1：关闭
	
	
	// 以下为版本更新的字段
	private int androidVersion; // Android 版本号
	
	private int iosVersion; // ios版本号

	private String androidAppUrl; // Android App的下载地址

	private String iosAppUrl; // IOS App 的下载地址

	private String androidExplain; // Android 说明

	private String iosExplain; // ios 说明

	private int pcVersion;// pc版本号

	private String pcAppUrl;// pc 软件的下载地址

	private String pcExplain;// pc 说明

	private int macVersion;// mac版本号

	private String macAppUrl;// mac 软件的下载地址

	private String macExplain;// mac 说明

	private String androidDisable;// android禁用版本号（凡低于此版本号的禁用）

	private String iosDisable;// ios禁用版本号（凡低于此版本号的禁用）

	private String pcDisable;// pc禁用版本号（凡低于此版本号的禁用）

	private String macDisable;// mac禁用版本号（凡低于此版本号的禁用）
	
	public String popularAPP;// 热门应用,1:开启，0：关闭， 示例:{\"lifeCircle\":1,\"videoMeeting\":1,\"liveVideo\":1,\"shortVideo\":0,\"peopleNearby\":0,\"scan\":0}",
	
	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
}
