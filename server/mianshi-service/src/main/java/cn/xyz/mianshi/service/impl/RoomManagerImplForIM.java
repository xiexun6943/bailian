package cn.xyz.mianshi.service.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mongodb.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cn.xyz.commons.constants.KConstants;
import cn.xyz.commons.constants.MsgType;
import cn.xyz.commons.ex.ServiceException;
import cn.xyz.commons.support.Callback;
import cn.xyz.commons.support.mongo.MongoOperator;
import cn.xyz.commons.utils.DateUtil;
import cn.xyz.commons.utils.ExcelUtil;
import cn.xyz.commons.utils.ReqUtil;
import cn.xyz.commons.utils.StringUtil;
import cn.xyz.commons.utils.ThreadUtil;
import cn.xyz.commons.vo.JSONMessage;
import cn.xyz.mianshi.model.PageResult;
import cn.xyz.mianshi.model.PageVO;
import cn.xyz.mianshi.model.RoomVO;
import cn.xyz.mianshi.service.RoomManager;
import cn.xyz.mianshi.utils.ConstantUtil;
import cn.xyz.mianshi.utils.SKBeanUtils;
import cn.xyz.mianshi.vo.Config;
import cn.xyz.mianshi.vo.GroupHelper;
import cn.xyz.mianshi.vo.GroupHelper.KeyWord;
import cn.xyz.mianshi.vo.Helper;
import cn.xyz.mianshi.vo.OfflineOperation;
import cn.xyz.mianshi.vo.Role;
import cn.xyz.mianshi.vo.Room;
import cn.xyz.mianshi.vo.Room.Member;
import cn.xyz.mianshi.vo.Room.Notice;
import cn.xyz.mianshi.vo.Room.Share;
import cn.xyz.mianshi.vo.User;
import cn.xyz.service.KXMPPServiceImpl;
import cn.xyz.service.KXMPPServiceImpl.MessageBean;
import cn.xyz.service.RedisServiceImpl;

@Service(RoomManager.BEAN_ID)
public class RoomManagerImplForIM extends MongoRepository<Room, ObjectId> implements RoomManager {
	
	public static final String  SHIKU_ROOMJIDS_USERID = "shiku_roomJids_userId";
	
	@Override
	public Datastore getDatastore() {
		return SKBeanUtils.getImRoomDatastore();
	}

	@Override
	public Class<Room> getEntityClass() {
		return Room.class;
	}
	
	private static UserManagerImpl getUserManager(){
		UserManagerImpl userManager = SKBeanUtils.getUserManager();
		return userManager;
	};
	
	private static RedisServiceImpl getRedisServiceImpl(){
		return SKBeanUtils.getRedisService();
	}
	
//	private final String roomMemerList="roomMemerList:";
	
	public final  String mucMsg="mucmsg_"; 
	/*int num=300000;
	int voide=350000;*/
	
	
	
	@Override
	public Room add(User user, Room entity, List<Integer> memberUserIdList) {
		Config config = SKBeanUtils.getSystemConfig();
		
		user.setNum(user.getNum()+1);
		if(null==entity.getId())
			entity.setId(ObjectId.get());
		/*entity.setCall("0"+user.getUserId()+user.getNum());*/
		entity.setCall(String.valueOf(getUserManager().createCall()));
		entity.setVideoMeetingNo(String.valueOf(getUserManager().createvideoMeetingNo()));
		entity.setSubject("");
		entity.setTags(Lists.newArrayList());
		entity.setNotice(new Room.Notice());
		entity.setNotices(Lists.newArrayList());
		entity.setUserSize(0);
		// entity.setMaxUserSize(1000);
		entity.setMembers(Lists.newArrayList());
	
		entity.setUserId(user.getUserId());
		entity.setNickname(user.getNickname());
		entity.setCreateTime(DateUtil.currentTimeSeconds());
		entity.setModifyTime(entity.getCreateTime());
		entity.setS(1);
		if(config.getMaxUserSize() != 0)
			entity.setMaxUserSize(config.getMaxUserSize());
		if(config.getIsAttritionNotice() != -1)
			entity.setIsAttritionNotice(config.getIsAttritionNotice());
		if(config.getIsLook() != -1)
			entity.setIsLook(config.getIsLook());
		if(config.getShowRead() != -1)
			entity.setShowRead(config.getShowRead());
		if(config.getIsNeedVerify() != -1)
			entity.setIsNeedVerify(config.getIsNeedVerify());
		if(config.getShowMember() != -1)
			entity.setShowMember(config.getShowMember());
		if(config.getAllowSendCard() != -1)
			entity.setAllowSendCard(config.getAllowSendCard());
		if(config.getAllowInviteFriend() != -1)
			entity.setAllowInviteFriend(config.getAllowInviteFriend());
		if(config.getAllowUploadFile() != -1)
			entity.setAllowUploadFile(config.getAllowUploadFile());
		if(config.getAllowConference() != -1)
			entity.setAllowConference(config.getAllowConference());
		if(config.getAllowSpeakCourse() != -1)
			entity.setAllowSpeakCourse(config.getAllowSpeakCourse());
		List<Role> userRoles = SKBeanUtils.getRoleManager().getUserRoles(user.getUserId(), null, 0);
		if(null != userRoles && userRoles.size()>0){
			for(Role role : userRoles){
				if(role.getRole() == 4){
					entity.setPromotionUrl(role.getPromotionUrl());
				}
			}
		}
		if(null == entity.getCategory())
			entity.setCategory(0);
		else
			entity.setCategory(entity.getCategory());
		if (null == entity.getName())
			entity.setName("????????????");
		if (null == entity.getDesc())
			entity.setDesc("");
		if (null == entity.getCountryId())
			entity.setCountryId(0);
		if (null == entity.getProvinceId())
			entity.setProvinceId(0);
		if (null == entity.getCityId())
			entity.setCityId(0);
		if (null == entity.getAreaId())
			entity.setAreaId(0);
		if (null == entity.getLongitude())
			entity.setLongitude(0d);
		if (null == entity.getLatitude())
			entity.setLatitude(0d);

		// ??????????????????
		getRoomDatastore().save(entity);
		
		// ?????????
		Member member = new Member();
		member.setActive(DateUtil.currentTimeSeconds());
		member.setCreateTime(member.getActive());
		member.setModifyTime(0L);
		member.setNickname(user.getNickname());
		member.setRole(1);
		member.setRoomId(entity.getId());
		member.setSub(1);
		member.setTalkTime(0L);
		member.setCall(entity.getCall());
		member.setVideoMeetingNo(entity.getVideoMeetingNo());
		member.setUserId(user.getUserId());
		
		MessageBean messageBean=null;
		//??????????????????
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("showRead", entity.getShowRead());
		jsonObject.put("lsLook", entity.getIsLook());
		jsonObject.put("isNeedVerify", entity.getIsNeedVerify());
		jsonObject.put("showMember", entity.getShowMember());
		jsonObject.put("allowSendCard", entity.getAllowSendCard());
		
		
		// ??????????????????
		List<Member> memberList = Lists.newArrayList(member);
		
		//?????????????????????
		if(null == memberUserIdList ||memberUserIdList.isEmpty()){
			messageBean=new MessageBean();
			messageBean.setType(KXMPPServiceImpl.NEW_MEMBER);
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(user.getNickname());
			messageBean.setToUserId(user.getUserId().toString());
			messageBean.setFileSize(entity.getShowRead());
			messageBean.setContent(entity.getName());
			messageBean.setToUserName(user.getNickname());
			messageBean.setFileName(entity.getId().toString());
			messageBean.setObjectId(entity.getJid());
			
			messageBean.setOther(jsonObject.toJSONString());
			messageBean.setMessageId(StringUtil.randomUUID());
			// ???????????????????????????????????? ??????
			sendChatToOneGroupMsg(user.getUserId(), entity.getJid(), messageBean);
			/**
			 * ?????? ????????????????????? jid  ??????
			 */
			SKBeanUtils.getRedisService().deleteUserRoomJidList(user.getUserId());
			if(0==SKBeanUtils.getUserManager().getOnlinestateByUserId(user.getUserId())) {
				SKBeanUtils.getRedisService().addRoomPushMember(entity.getJid(), user.getUserId());
			}
		}else if (null != memberUserIdList && !memberUserIdList.isEmpty()) {
			// ???????????????????????????
			Long currentTimeSeconds = DateUtil.currentTimeSeconds();
			ObjectId roomId = entity.getId();
			//????????????
			memberUserIdList.add(user.getUserId());
			Member _member =null;
			for (int userId : memberUserIdList) {
				User _user = getUserManager().getUser(userId);
				
				//??????????????????????????????
				if(userId!=member.getUserId()){
					//??????
						_member= new Member();
						_member.setActive(currentTimeSeconds);
						_member.setCreateTime(currentTimeSeconds);
						_member.setModifyTime(0L);
						_member.setNickname(_user.getNickname());
						_member.setRole(3);
						_member.setRoomId(roomId);
						_member.setSub(1);
						_member.setCall(entity.getCall());
						_member.setVideoMeetingNo(entity.getVideoMeetingNo());
						_member.setTalkTime(0L);
						_member.setUserId(_user.getUserId());
						
						memberList.add(_member);
				}
					
				//xmpp??????
				messageBean=new MessageBean();
				messageBean.setType(KXMPPServiceImpl.NEW_MEMBER);
				messageBean.setFromUserId(user.getUserId().toString());
				messageBean.setFromUserName(user.getNickname());
				messageBean.setToUserId(_user.getUserId().toString());
				messageBean.setFileSize(entity.getShowRead());
				messageBean.setContent(entity.getName());
				messageBean.setToUserName(_user.getNickname());
				messageBean.setFileName(entity.getId().toString());
				messageBean.setObjectId(entity.getJid());
				
				messageBean.setOther(jsonObject.toJSONString());
				messageBean.setMsgType(0);// ??????
				messageBean.setMessageId(StringUtil.randomUUID());
				// ???????????????????????????????????? ??????
				sendChatToOneGroupMsg(user.getUserId(), entity.getJid(), messageBean);
				/*try {
					KXMPPServiceImpl.getInstance().send(messageBean);
				} catch (Exception e) {
					e.printStackTrace();
				}*/
				/**
				 * ?????? ????????????????????? jid  ??????
				 */
				SKBeanUtils.getRedisService().deleteUserRoomJidList(userId);
				if(0==SKBeanUtils.getUserManager().getOnlinestateByUserId(userId)) {
					SKBeanUtils.getRedisService().addRoomPushMember(entity.getJid(), userId);
				}
			}
		}
		// ??????????????????
		getRoomDatastore().save(memberList);

		updateUserSize(entity.getId(), memberList.size());
		
		// ?????????????????????
		saveJidsByUserId(user.getUserId(), queryUserRoomsJidList(user.getUserId()));
		// ????????????????????????????????????
		updateOfflineOperation(user.getUserId(), entity.getId());
		return entity;
	}

	/** @Description:????????????????????????????????????
	* @param userId
	* @param roomId
	**/ 
	public void updateOfflineOperation(Integer userId,ObjectId roomId){
		Datastore datastore = SKBeanUtils.getDatastore();
		Query<OfflineOperation> query = datastore.createQuery(OfflineOperation.class).field("userId").equal(userId).field("friendId").equal(String.valueOf(roomId));
		if(null == query.get()){
			datastore.save(new OfflineOperation(userId, KConstants.MultipointLogin.TAG_ROOM, String.valueOf(roomId), DateUtil.currentTimeSeconds()));
		}else{
			UpdateOperations<OfflineOperation> ops = datastore.createUpdateOperations(OfflineOperation.class);
			ops.set("operationTime", DateUtil.currentTimeSeconds());
			datastore.update(query, ops);
		}
	}
	
	public List<Integer> getRoomPushUserIdList(ObjectId roomId){
		BasicDBObject query=new BasicDBObject("roomId", roomId);
		query.append("offlineNoPushMsg", new BasicDBObject(MongoOperator.NE, 1));
		List<Integer> memberIdList=distinct("shiku_room_member","userId", query);
		return  memberIdList;
	}
	
	@Override
	public void delete(ObjectId roomId,Integer userId) {
		Query<Room> query = getRoomDatastore().createQuery(getEntityClass()).field("_id").equal(roomId);
		Room room =query.get();
		if(null==room){
			System.out.println("====> RoomManagerImplForIM > delete room is null ");
			return;
		}
		Member member = getMember(roomId, userId);

		List<Integer> userRoles = SKBeanUtils.getRoleManager().getUserRoles(userId);
		if(null != member){
			if(!userRoles.contains(5) && !userRoles.contains(6)){
				if(1 != member.getRole())
					throw new ServiceException("????????????????????????");
			}
		}else{
			if(!userRoles.contains(5) && !userRoles.contains(6))
				throw new ServiceException("????????????????????????");
				
		}
		String roomJid=room.getJid();
		if(room.getUserSize() >0){
			MessageBean messageBean = new MessageBean();
			messageBean.setFromUserId(room.getUserId() + "");
			messageBean.setFromUserName(getMemberNickname(roomId, room.getUserId()));
			messageBean.setType(KXMPPServiceImpl.DELETE_ROOM);
			messageBean.setObjectId(room.getJid());
			messageBean.setContent(room.getName());
			messageBean.setMessageId(StringUtil.randomUUID());
			// ??????????????????
			sendChatGroupMsg(roomId, room.getJid(), messageBean);
		}
		
//		int createUserId=room.getUserId();
		ThreadUtil.executeInThread(new Callback() {
			@Override
			public void execute(Object obj) {
				getRoomDatastore().delete(query);
				List<Integer> memberIdList = getMemberIdList(roomId);
				for (Integer id : memberIdList) {
					// ???????????????????????? Jids ??????
					SKBeanUtils.getRedisService().deleteUserRoomJidList(id);
				}
				//???????????? ?????? ????????????
				Query<Member> merQuery = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId);
				getRoomDatastore().delete(merQuery);
				getRoomDatastore().getDB().getCollection(mucMsg+roomJid).drop();
//				SKBeanUtils.getRedisCRUD().del(roomMemerList+roomId);
				
				//????????????
				Query<Notice> notQuery = getRoomDatastore().createQuery(Room.Notice.class).field("roomId").equal(roomId);
				getRoomDatastore().delete(notQuery);
				
				//??????????????????????????????
				deleMucHistory(roomJid);

				// ?????? ?????????????????? ??? ????????????????????????
				destroyRoomMsgFileAndShare(roomId, roomJid);
				
				// ?????????????????????????????????
				getUserManager().delReport(null, roomId.toString());
				
				/**
				 * tigase 8.0??? ?????? ????????????
				 */
				//destroyRoomToIM(roomJid);
				User user = getUserManager().getUser(userId);
				destroyRoomToIM(user.getUserId()+"", user.getPassword(), roomJid);
				List<Member> memberList = getRedisServiceImpl().getMemberList(roomId.toString());
				memberList.forEach(member ->{
					// ???????????????????????????jids
					saveJidsByUserId(member.getUserId(), queryUserRoomsJidList(member.getUserId()));
				});
				// ??????????????????????????????
				updateRoomInfoByRedis(roomId.toString());
				getRedisServiceImpl().deleteNoticeList(roomId.toString());
			}
		});
		// ????????????????????????????????????
		updateOfflineOperation(userId, roomId);
	}
	
	/**
	* @Description: TODO(????????????)
	* @param @param roomId  ??????ID
	* @param @param talkTime   ??????????????????   0 ????????????
	 */
	public void roomAllBanned(ObjectId roomId,long talkTime){
		ThreadUtil.executeInThread(new Callback() {
			@Override
			public void execute(Object obj) {
				Query<Member> query = getRoomDatastore().createQuery(Member.class);
				query.filter("roomId", roomId);
				UpdateOperations<Member> operations = getRoomDatastore().createUpdateOperations(Member.class);
				operations.set("talkTime", talkTime);
				getRoomDatastore().update(query,operations);
			}
		});
		
	}
	
	
	public synchronized JSONMessage update(User user,RoomVO roomVO,int isAdmin,int isConsole) {
		JSONMessage jsonMessage = JSONMessage.success();
		
		Query<Room> query = getRoomDatastore().createQuery(getEntityClass());
		query.filter("_id", roomVO.getRoomId());
		
		UpdateOperations<Room> operations = getRoomDatastore().createUpdateOperations(getEntityClass());

		Room room = getRoom(roomVO.getRoomId());
		if(0 == isConsole){
			if(null != room && room.getS() == -1)
				throw new ServiceException("???????????????????????????");
		}
		if (!StringUtil.isEmpty(roomVO.getRoomName())&&(!room.getName().equals(roomVO.getRoomName()))) {
			UpdateGroupNickname(query,user, roomVO, isAdmin,room,operations);
			return jsonMessage;
		}
		/*????????????*/
		if(-2<roomVO.getTalkTime()){
			allBannedSpeak(query,user, roomVO, room, operations);
			return jsonMessage;
		}

		if (!StringUtil.isEmpty(roomVO.getDesc())) {
			operations.set("desc", roomVO.getDesc());
		}
		if (!StringUtil.isEmpty(roomVO.getSubject())) {
			operations.set("subject", roomVO.getSubject());
		}
		try {
			if (!StringUtil.isEmpty(roomVO.getNotice())) {
				if (getMember(room.getId(),ReqUtil.getUserId()).getRole() == 3) {
					return JSONMessage.failure("?????????????????????????????????????????????!");
				}
				String noticeId = newNotice(query,user, roomVO, isAdmin, room, operations);
				return JSONMessage.success(noticeId);
			}
		} catch (Exception e) {
				e.printStackTrace();
			}
		if(-1<roomVO.getShowRead()&&room.getShowRead()!=roomVO.getShowRead()){
			alreadyReadNums(query,user, roomVO, isAdmin, room, operations);
			return jsonMessage;
		}
		if(-1 != roomVO.getIsNeedVerify()){
			groupVerification(query,user, roomVO, isAdmin, room, operations);
			return jsonMessage;
		}
		if(-1!=roomVO.getIsLook()){
			roomIsPublic(query,user, roomVO, isAdmin, room, operations);
			return jsonMessage;
		}
		if(null != roomVO.getMaxUserSize() && roomVO.getMaxUserSize()>=0){
			if(roomVO.getMaxUserSize() < room.getUserSize())
				throw new ServiceException("????????????????????????????????????????????????");
			int maxUserSize = SKBeanUtils.getAdminManager().getConfig().getMaxUserSize();
			if(roomVO.getMaxUserSize() > maxUserSize)
				throw new ServiceException("???????????????????????????"+maxUserSize);
			operations.set("maxUserSize",roomVO.getMaxUserSize());
		}
		// ???????????????????????????
		if(null != roomVO.getS() && 0 != roomVO.getS()){
			roomIsLocking(query, user, roomVO, isAdmin, room, operations);
			return jsonMessage;
		}
			
		if(-1!=roomVO.getShowMember()){
			showMember(query,user, roomVO, isAdmin, room, operations);
			return jsonMessage;
		}
		if(-1!=roomVO.getAllowSendCard()){
			roomAllowSendCard(query,user, roomVO, isAdmin, room, operations);
			return jsonMessage;
		}
		
		if(-1!=roomVO.getAllowInviteFriend()){
			roomAllowInviteFriend(query,user, roomVO, room, operations);
			return jsonMessage;
		}
		
		if(-1!=roomVO.getAllowUploadFile()){
			roomAllowUploadFile(query,user, roomVO, room, operations);
			return jsonMessage;
		}
		
		if(-1!=roomVO.getAllowConference()){
			roomAllowConference(query,user, roomVO, room, operations);
			return jsonMessage;
		}
		
		if(-1!=roomVO.getAllowSpeakCourse()){
			roomAllowSpeakCourse(query,user, roomVO, room, operations);
			return jsonMessage;
		}
	
		if(-1!=roomVO.getAllowHostUpdate())
			operations.set("allowHostUpdate",roomVO.getAllowHostUpdate());
		
		if(0!=roomVO.getChatRecordTimeOut())// ??????????????????
			ChatRecordTimeOut(query,user,roomVO,room,operations);
			
		
		if(-1!=roomVO.getIsAttritionNotice())
			operations.set("isAttritionNotice",roomVO.getIsAttritionNotice());

		operations.set("modifyTime", DateUtil.currentTimeSeconds());
		
		
		synchronized (query) {
			getRoomDatastore().update(query, operations);
		}
		// ????????????????????????
		getRedisServiceImpl().deleteRoom(roomVO.getRoomId().toString());
		return jsonMessage;
	}
	
	/** @Description:???????????????????????? ?????? 
	* @param roomId
	**/ 
	protected void updateRoomInfoByRedis(String roomId){
		getRedisServiceImpl().deleteRoom(roomId);
		getRedisServiceImpl().deleteMemberList(roomId);
	}
	
	// ???????????????
	public synchronized void UpdateGroupNickname(Query<Room> query,User user,RoomVO roomVO,int isAdmin,Room room,UpdateOperations<Room> operations) {
		
		operations.set("name", roomVO.getRoomName());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		if (1 == isAdmin) {
			// IMPORTANT 1-2?????????????????????-??????
			messageBean.setFromUserId(user.getUserId() + "");
			messageBean.setFromUserName(("10005".equals(user.getUserId().toString())?"???????????????":getMemberNickname(room.getId(), user.getUserId())));
			messageBean.setType(KXMPPServiceImpl.CHANGE_ROOM_NAME);
			messageBean.setObjectId(room.getJid());
			messageBean.setContent(roomVO.getRoomName());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
		
	}
	
	// ????????????
	public void allBannedSpeak(Query<Room> query,User user,RoomVO roomVO,Room room,UpdateOperations<Room> operations){
		operations.set("talkTime", roomVO.getTalkTime());
		updateGroup(query, operations);
		roomAllBanned(roomVO.getRoomId(), roomVO.getTalkTime());
		/**
		 * ??????????????????????????????
		 */
		updateRoomInfoByRedis(room.getId().toString());
		MessageBean messageBean=new MessageBean();
		messageBean.setType(KXMPPServiceImpl.RoomAllBanned);
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
		messageBean.setContent(String.valueOf(roomVO.getTalkTime()));
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ??????????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ?????????
	public String newNotice(Query<Room> query,User user,RoomVO roomVO,int isAdmin,Room room,UpdateOperations<Room> operations){
		Notice notice = new Notice(new ObjectId(),roomVO.getRoomId(),roomVO.getNotice(),user.getUserId(),user.getNickname());
		// ??????????????????
		operations.set("notice", notice);
		updateGroup(query, operations);
		// ????????????????????????
		getRoomDatastore().save(notice);
		/**
		 * ????????????
		 */
		getRedisServiceImpl().deleteNoticeList(room.getId());
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		if (1 == isAdmin) {
			// IMPORTANT 1-5??????????????????-??????
			messageBean.setFromUserId(user.getUserId() + "");
			messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
			messageBean.setType(KXMPPServiceImpl.NEW_NOTICE);
			messageBean.setObjectId(room.getJid());
			messageBean.setContent(roomVO.getNotice());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
		return notice.getId().toString();
	}
	
	// ??????????????????
	public void alreadyReadNums(Query<Room> query,User user,RoomVO roomVO,int isAdmin,Room room,UpdateOperations<Room> operations){
		operations.set("showRead", roomVO.getShowRead());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean=new MessageBean();
		if(1==isAdmin){
			messageBean.setType(KXMPPServiceImpl.SHOWREAD);
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
			messageBean.setContent(String.valueOf(roomVO.getShowRead()));
			messageBean.setObjectId(room.getJid());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ????????????
	public void groupVerification(Query<Room> query,User user,RoomVO roomVO,int isAdmin,Room room,UpdateOperations<Room> operations){
		operations.set("isNeedVerify",roomVO.getIsNeedVerify());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean=new MessageBean();
		if(1==isAdmin){
			messageBean.setType(KXMPPServiceImpl.RoomNeedVerify);
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
			messageBean.setContent(String.valueOf(roomVO.getIsNeedVerify()));
			messageBean.setObjectId(room.getJid());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ??????????????????
	public void roomIsPublic(Query<Room> query,User user,RoomVO roomVO,int isAdmin,Room room,UpdateOperations<Room> operations){
		operations.set("isLook",roomVO.getIsLook());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean=new MessageBean();
		if(1==isAdmin){
			messageBean.setType(KXMPPServiceImpl.RoomIsPublic);
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
			messageBean.setContent(String.valueOf(roomVO.getIsLook()));
			messageBean.setObjectId(room.getJid());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ?????????????????????
	public void roomIsLocking(Query<Room> query,User user,RoomVO roomVO,int isAdmin,Room room,UpdateOperations<Room> operations){
		operations.set("s",roomVO.getS());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean=new MessageBean();
		if(1==isAdmin){
			messageBean.setType(KXMPPServiceImpl.consoleProhibitRoom);
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
			messageBean.setContent(roomVO.getS());
			messageBean.setObjectId(room.getJid());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendChatGroupMsg(roomVO.getRoomId(), room.getJid(), messageBean);
	}
	
	
	// ????????????????????????
	public void roomAllowSendCard(Query<Room> query,User user,RoomVO roomVO,int isAdmin,Room room,UpdateOperations<Room> operations){
		operations.set("allowSendCard",roomVO.getAllowSendCard());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean=new MessageBean();
		if(1==isAdmin){
			messageBean.setType(KXMPPServiceImpl.RoomAllowSendCard);
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
			messageBean.setContent(String.valueOf(roomVO.getAllowSendCard()));
			messageBean.setObjectId(room.getJid());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ???????????? ?????????????????? ??????????????????
	public void showMember(Query<Room> query,User user, RoomVO roomVO, int isAdmin, Room room, UpdateOperations<Room> operations) {
		operations.set("showMember", roomVO.getShowMember());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		if (1 == isAdmin) {
			messageBean.setType(KXMPPServiceImpl.RoomShowMember);
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
			messageBean.setContent(String.valueOf(roomVO.getShowMember()));
			messageBean.setObjectId(room.getJid());
			messageBean.setMessageId(StringUtil.randomUUID());
		}
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ?????????????????????????????????
	public void roomAllowInviteFriend(Query<Room> query,User user, RoomVO roomVO, Room room, UpdateOperations<Room> operations) {
		operations.set("allowInviteFriend", roomVO.getAllowInviteFriend());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		messageBean.setType(KXMPPServiceImpl.RoomAllowInviteFriend);
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
		messageBean.setContent(String.valueOf(roomVO.getAllowInviteFriend()));
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(UUID.randomUUID().toString());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ?????????????????????????????????
	public void roomAllowUploadFile(Query<Room> query,User user, RoomVO roomVO, Room room, UpdateOperations<Room> operations) {
		operations.set("allowUploadFile", roomVO.getAllowUploadFile());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		messageBean.setType(KXMPPServiceImpl.RoomAllowUploadFile);
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
		messageBean.setContent(String.valueOf(roomVO.getAllowUploadFile()));
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ??????????????????????????????
	public void roomAllowConference(Query<Room> query,User user, RoomVO roomVO, Room room, UpdateOperations<Room> operations) {
		operations.set("allowConference", roomVO.getAllowConference());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		messageBean.setType(KXMPPServiceImpl.RoomAllowConference);
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
		messageBean.setContent(String.valueOf(roomVO.getAllowConference()));
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	//  ??????????????????????????????
	public void roomAllowSpeakCourse(Query<Room> query,User user, RoomVO roomVO, Room room, UpdateOperations<Room> operations) {
		operations.set("allowSpeakCourse", roomVO.getAllowSpeakCourse());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		messageBean.setType(KXMPPServiceImpl.RoomAllowSpeakCourse);
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
		messageBean.setContent(String.valueOf(roomVO.getAllowSpeakCourse()));
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
	}
	
	// ???????????????????????? ??????
	public void ChatRecordTimeOut(Query<Room> query,User user,RoomVO roomVO,Room room,UpdateOperations<Room> operations){
		operations.set("chatRecordTimeOut",roomVO.getChatRecordTimeOut());
		updateGroup(query, operations);
		/**
		 * ??????????????????
		 */
		getRedisServiceImpl().deleteRoom(room.getId().toString());
		MessageBean messageBean = new MessageBean();
		messageBean.setType(KXMPPServiceImpl.ChatRecordTimeOut);
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(room.getId(), user.getUserId()));
		messageBean.setContent(String.valueOf(roomVO.getChatRecordTimeOut()));
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(StringUtil.randomUUID());
		
		// ????????????
		sendGroupMsg(room.getJid(), messageBean);
		
	}
	
	public synchronized void updateGroup(Query<Room> query,UpdateOperations<Room> operations){
		getRoomDatastore().update(query, operations);
	}
	
	// ?????????????????????
	public void sendGroupOne(Integer userIds,MessageBean messageBean){
		try {
			messageBean.setMsgType(0);
			KXMPPServiceImpl.getInstance().send(messageBean);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// ??????????????????
	public void sendGroupMsg(String jid,MessageBean messageBean){
		try {
			KXMPPServiceImpl.getInstance().sendMsgToGroupByJid(jid,messageBean);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// ??????????????????????????? ,??? ??????????????????
	public void sendChatToOneGroupMsg(Integer userIds, String jid,MessageBean messageBean){
		try {
			// ????????????
			messageBean.setMsgType(0);
			messageBean.setMessageId(StringUtil.randomUUID());
			KXMPPServiceImpl.getInstance().send(messageBean);
			// ????????????
			ThreadUtil.executeInThread(new Callback() {
				
				@Override
				public void execute(Object obj) {
					try {
						KXMPPServiceImpl.getInstance().sendMsgToGroupByJid(jid, messageBean);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// ????????????????????????????????? ,??? ??????????????????
	public void sendChatGroupMsg(ObjectId roomId,String jid,MessageBean messageBean){
		try {
			// ????????????
			messageBean.setMsgType(0);
			messageBean.setMessageId(StringUtil.randomUUID());
			KXMPPServiceImpl.getInstance().send(messageBean,getMemberIdList(roomId));
			// ????????????
			ThreadUtil.executeInThread(new Callback() {
				
				@Override
				public void execute(Object obj) {
					try {
						KXMPPServiceImpl.getInstance().sendMsgToGroupByJid(jid, messageBean);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	* @Description: TODO(?????? ??????)
	* @param @param roomId  ??????ID
	* @param @param toUserId   ????????? ??????ID   ?????? ??? ????????????
	 */
	public Room transfer(Room room,Integer toUserId){
		
		String nickName = getUserManager().getNickName(toUserId);
		Query<Room> roomQuery = getRoomDatastore().createQuery(getEntityClass()).filter("_id", room.getId());
		UpdateOperations<Room> roomOps = getRoomDatastore().createUpdateOperations(getEntityClass());
		roomOps.set("userId", toUserId);
		roomOps.set("nickname", nickName);
		getRoomDatastore().update(roomQuery, roomOps);
		
		/*?????? ??????????????????*/
		Query<Member> query = getRoomDatastore().createQuery(Member.class);
		query.filter("roomId", room.getId());
		query.filter("userId", room.getUserId());
		UpdateOperations<Member> operations = getRoomDatastore().createUpdateOperations(Member.class);
		operations.set("role", 3);
		getRoomDatastore().update(query,operations);
		
		/*????????????????????????*/
		query=getRoomDatastore().createQuery(Member.class);
		query.filter("roomId", room.getId());
		query.filter("userId",toUserId);
		operations = getRoomDatastore().createUpdateOperations(Member.class);
		operations.set("role", 1);
		getRoomDatastore().update(query, operations);
		// ????????????????????????????????????
		updateRoomInfoByRedis(room.getId().toString());
		MessageBean message=new MessageBean();
		message.setType(KXMPPServiceImpl.RoomTransfer);
		message.setFromUserId(room.getUserId().toString());
		message.setFromUserName(getMemberNickname(room.getId(), room.getUserId()));
		message.setObjectId(room.getJid());
		message.setToUserId(toUserId.toString());
		message.setToUserName(getUserManager().getNickName(toUserId));
		message.setMessageId(StringUtil.randomUUID());
		// ????????????????????????????????????????????????
		sendChatToOneGroupMsg(toUserId, room.getJid(), message);
		return get(room.getId());
	}
	
	
	@Override
	public Room get(ObjectId roomId,Integer pageIndex,Integer pageSize) {
		// redis room ????????? members noties
		Room redisRoom = SKBeanUtils.getRedisService().queryRoom(roomId);
		if(null != redisRoom){
			if(-1 == redisRoom.getS())
				throw new ServiceException("????????????????????????");
			Room specialRoom = specialHandleByRoom(redisRoom, roomId,pageIndex,pageSize);
			return specialRoom;
		}else{
			Room room = getRoomDatastore().createQuery(getEntityClass()).field("_id").equal(roomId).get();
			if(null != room && -1 == room.getS())
				throw new ServiceException("????????????????????????");
			if(null==room)
				throw new ServiceException("??????????????????");
			Room specialRoom = specialHandleByRoom(room, roomId,pageIndex,pageSize);
			return specialRoom;
		}
	}

	@Override
	public Room get(String roomJid) {
		Room room = getRoomDatastore().createQuery(getEntityClass()).field("jid").equal(roomJid).get();
		if(null != room && -1 == room.getS())
			throw new ServiceException("????????????????????????");
		if(null==room)
			throw new ServiceException("??????????????????");
		Room specialRoom = specialHandleByRoom(room, room.getId(),0,1);
		return specialRoom;
	}

	/** @Description: ??????????????????????????????
	* @param room
	* @param roomId
	* @return
	**/ 
	public Room specialHandleByRoom(Room room,ObjectId roomId,Integer pageIndex,Integer pageSize){
		// ??????????????????
		Member member = SKBeanUtils.getRoomManagerImplForIM().getMember(roomId, ReqUtil.getUserId());
		if(null == member){
			// ?????????????????????????????????????????????????????????????????????members
			Room joinRoom = getRoom(roomId);
//			List<Member> members = getMembers(roomId,pageIndex,pageSize);
			List<Member> members = getHeadMemberListByPageImpls(roomId, pageSize);
			joinRoom.setMembers(members);
			return joinRoom;
		}
		int role = member.getRole();
		List<Member> members = null;
//		 ?????????????????????????????????  ????????????????????????
		if(1 != member.getRole()){
			Query<Member> query = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).order("role").order("createTime").offset(pageIndex*pageSize).limit(pageSize);
			if(role > 1 && role < 4){
				Query<Member> queryMemberSize = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).offset(pageIndex*pageSize).limit(pageSize);
				query.field("role").lessThan(4).order("role");
				members = query.asList();
				int specialSize = queryMemberSize.field("role").greaterThanOrEq(4).asList().size();// ??????????????????
				room.setUserSize(room.getUserSize()-specialSize);
			}else if(role == 4 || role == 5){
				// ?????????
				query.or(query.criteria("role").lessThan(4),query.criteria("userId").equal(ReqUtil.getUserId()));
				members = query.asList();
				room.setUserSize(members.size());
			}
			room.setMembers(members);
		}else {
//			List<Member> membersList = getMembers(roomId,pageIndex,pageSize);
			List<Member> membersList = getHeadMemberListByPageImpls(roomId, pageSize);
			room.setMembers(membersList);
		}
		// ?????????
		List<Notice> noticesCache = getRedisServiceImpl().getNoticeList(roomId);
		if(null != noticesCache && noticesCache.size() > 0){
			room.setNotices(noticesCache);
		}else{
			List<Notice> noticesDB = getRoomDatastore().createQuery(Room.Notice.class).field("roomId").equal(roomId).order("-time").asList();
			room.setNotices(noticesDB);
			/**
			 * ???????????????????????????
			 */
			getRedisServiceImpl().saveNoticeList(roomId, noticesDB);
		}
		return room;
	}
	
	/** @Description: ????????????????????????????????????????????????????????? 
	* @param roomId
	* @param pageIndex
	* @param pageSize
	* @return
	**/ 
	@SuppressWarnings("deprecation")
	public List<Member> getMembers(ObjectId roomId,Integer pageIndex,Integer pageSize){
		List<Member> members = new ArrayList<Member>();
		// ?????????
		List<Member> memberCacheList = getRedisServiceImpl().getMemberList(roomId.toString(),pageIndex,pageSize);
		if(null != memberCacheList && memberCacheList.size() > 0)
			members = memberCacheList;
		else{
			// ???????????????
			Query<Member> memberQuery = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("role").lessThanOrEq(2).order("role").order("createTime");
			List<Member> adminList = memberQuery.asList();
			// ????????????
			Query<Member> query = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("role").greaterThan(2).order("createTime");
			
			List<Member> memberAllList = query.asList();
			int adminSize = adminList.size();
			if(pageSize > adminSize){
				pageSize -= adminSize;
//				Query<Member> query = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("role").greaterThan(2).order("createTime").offset(pageIndex*pageSize).limit(pageSize);
				query.offset(pageIndex * pageSize).limit(pageSize);
				List<Member> memberList = query.asList();// ???????????????
				members.addAll(adminList);
				members.addAll(memberList);
			} else {
				Query<Member> limit = memberQuery.offset(pageIndex * pageSize).limit(pageSize);
				members = limit.asList();
			}
			List<Member> dbMembers = new ArrayList<Member>();
			dbMembers.addAll(adminList);
			dbMembers.addAll(memberAllList);
			// ???????????????????????????
			getRedisServiceImpl().saveMemberList(roomId.toString(), dbMembers);
		}
		
		return members;
	}
	
	/** @Description:room/get ??? joinTime ???0?????????????????? ??????
	 * // ???????????? ??? ?????? ???pageSize = 100 ???  ??????????????? ??????pageSize{ ?????? + ????????? = 80???   ?????? 80+20???????????????} 
	 *  ??????????????? ????????????pageSize{ ?????? + ????????? = 120???   ?????? 120??? + 1?????????????????????????????????????????????createTime}
	* @param roomId
	* @param pageSize
	* @return
	**/ 
	@SuppressWarnings("deprecation")
	public List<Member> getHeadMemberListByPageImpls(ObjectId roomId,Integer pageSize){

		List<Member> members = new ArrayList<Member>();
		// ???????????????
		Query<Member> adminMemberQuery = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("role").lessThanOrEq(2).order("role").order("createTime");
		List<Member> adminList = adminMemberQuery.asList();
		int adminSize = adminList.size();
		if(adminSize < pageSize){
			// ??????pageSize
			members.addAll(adminMemberQuery.asList());
			Query<Member> lessMemberQuery = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("role").greaterThan(2).order("createTime").limit(pageSize - adminSize);
			members.addAll(lessMemberQuery.asList());
		}else{
			members.addAll(adminMemberQuery.asList());
			Query<Member> lessMemberQuery = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("role").greaterThan(2).order("createTime").limit(1);
			members.addAll(lessMemberQuery.asList());
		}
		return members;
	
	}
	
	/** @Description:???????????????
	* @param roomId
	* @param joinTime
	* @param pageSize
	* @return
	**/ 
	@SuppressWarnings("deprecation")
	public List<Member> getMemberListByPageImpl(ObjectId roomId,long joinTime,Integer pageSize){
		if(0 == joinTime)
			return getHeadMemberListByPageImpls(roomId, pageSize);
		Query<Member> memberQuery = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("role").greaterThan(2).field("createTime").greaterThanOrEq(joinTime).order("createTime").limit(pageSize);
		return memberQuery.asList();
	}
	
	public Room consoleGetRoom(ObjectId roomId) {
		Room room = getRoomDatastore().createQuery(getEntityClass()).field("_id").equal(roomId).get();

		if (null != room) {
			List<Member> members = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).order("createTime").order("role").asList();
			List<Notice> notices = getRoomDatastore().createQuery(Room.Notice.class).field("roomId").equal(roomId).order("-time").asList();

			room.setMembers(members);
			room.setNotices(notices);
			if(0==room.getUserSize()){
				room.setUserSize(members.size());
				DBObject q = new BasicDBObject("_id", roomId);
				DBObject o = new BasicDBObject("$set", new BasicDBObject("userSize", members.size()));
				getRoomDatastore().getCollection(getEntityClass()).update(q, o);
			}
		}
		return room;
	}
	
	
	/**
	* @Description: TODO(????????????????????????????????????????????????????????????????????????????????????????????????,)
	* @param @param roomId
	* @param @return    ??????
	 */
	public Room getRoom(ObjectId roomId){
		Room room = null;
		Room roomCache = getRedisServiceImpl().queryRoom(roomId);
		if(null != roomCache){
			room = roomCache;
		}else{
			Room roomDB = getRoomDatastore().createQuery(getEntityClass()).field("_id").equal(roomId).get();
			if(null == roomDB)
				throw new ServiceException("???????????????");
			room = roomDB;
			/**
			 * ?????? ??????
			 */
			getRedisServiceImpl().saveRoom(room);
		}
		// ????????????????????????
		room.setMembers(getAdministrationMemberList(roomId));
		return room;
	}
	public Integer getCreateUserId(ObjectId roomId){
		return (Integer) queryOneField("userId", new BasicDBObject("_id", roomId));
	}
	public ObjectId getRoomId(String jid) {
		return (ObjectId) queryOneField("_id", new BasicDBObject("jid", jid));
	}
	public String queryRoomJid(ObjectId roomId) {
		return (String) queryOneFieldById("jid",roomId);
	}
	public Integer queryRoomStatus(ObjectId roomId) {
		return (Integer) queryOneFieldById("s",roomId);
	}
	public String getRoomName(String jid) {
		return (String) queryOneField("name", new BasicDBObject("jid", jid));
	}
	public String getRoomName(ObjectId roomId) {
		return (String) queryOneField("name", new BasicDBObject("_id", roomId));
	}
	// ????????????
	public Integer getRoomStatus(ObjectId roomId) {
		return (Integer) queryOneField("s", new BasicDBObject("_id", roomId));
	}
	@Override
	public List<Room> selectList(int pageIndex, int pageSize, String roomName) {
		Query<Room> q = getRoomDatastore().createQuery(getEntityClass());
		if (!StringUtil.isEmpty(roomName)){
			//q.field("name").contains(roomName);
			q.or(q.criteria("name").containsIgnoreCase(roomName),
					q.criteria("desc").containsIgnoreCase(roomName));
		}
		q.filter("isLook", 0);
		List<Room> roomList = q.offset(pageIndex * pageSize).limit(pageSize).order("-_id").asList();
		return roomList;
	}
	/**
	* @Description: TODO(?????????????????????????????????jid)
	* @param @param userId
	* @param @return    ??????
	 */
	public List<String> queryUserRoomsJidList(int userId){
		List<ObjectId> roomIdList = queryUserRoomsIdList(userId);
		BasicDBObject query=new BasicDBObject("_id", new BasicDBObject(MongoOperator.IN, roomIdList));
		return getRoomDatastore().getCollection(getEntityClass()).distinct("jid", query);
	}
	
	/** @Description:??????SHIKU_ROOMJIDS_USERID ????????????????????????????????????jid
	* @param userId
	* @return
	**/ 
	public List<String> queryUserRoomsJidListByDB(int userId){
		DBCollection collection = getRoomDatastore().getDB().getCollection(SHIKU_ROOMJIDS_USERID);
		BasicDBObject query = new BasicDBObject("userId", userId);
		return collection.distinct("jids",query);
	}
	/**
	 * ??????????????????????????????  ??????Jid ??????
	 * @param userId
	 * @return
	 */
	public List<String> queryUserNoPushJidList(int userId){
		BasicDBObject query=new BasicDBObject("userId",userId);
		query.append("offlineNoPushMsg", 1);
		return getRoomDatastore().getCollection(Member.class).distinct("jid", query);
	}
	
	/**
	* @Description: TODO(?????????????????????????????????roomId)
	* @param @param userId
	* @param @return    ??????
	 */
	public List<ObjectId> queryUserRoomsIdList(int userId){
		BasicDBObject query=new BasicDBObject("userId", userId);
		return getRoomDatastore().getCollection(Member.class).distinct("roomId", query);
		 
	}

	@Override
	public Object selectHistoryList(int userId, int type) {
		List<Object> historyIdList = Lists.newArrayList();

		Query<Room.Member> q = getRoomDatastore().createQuery(Room.Member.class).field("userId").equal(userId);
		if (1 == type) {// ???????????????
			q.filter("role =", 1);
		} else if (2 == type) {// ???????????????
			q.filter("role !=", 1);
		}
		DBCursor cursor = getRoomDatastore().getCollection(Room.Member.class).find(q.getQueryObject(),
				new BasicDBObject("roomId", 1));
		while (cursor.hasNext()) {
			DBObject dbObj = cursor.next();
			historyIdList.add(dbObj.get("roomId"));
		}

		if (historyIdList.isEmpty())
			return null;

		List<Room> historyList = getRoomDatastore().createQuery(getEntityClass()).field("_id").in(historyIdList).order("-_id")
				.asList();
		historyList.forEach(room -> {
			Member member = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(room.getId())
					.field("userId").equal(userId).get();
			room.setMember(member);
		});

		return historyList;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Object selectHistoryList(int userId, int type, int pageIndex, int pageSize) {
		List<Object> historyIdList = Lists.newArrayList();

		Query<Room.Member> q = getRoomDatastore().createQuery(Room.Member.class).field("userId").equal(userId);
		if (1 == type) {// ???????????????
			q.filter("role =", 1);
		} else if (2 == type) {// ???????????????
			q.filter("role !=", 1);
		}
		DBCursor cursor = getRoomDatastore().getCollection(Room.Member.class).find(q.getQueryObject(),
				new BasicDBObject("roomId", 1));
		while (cursor.hasNext()) {
			DBObject dbObj = cursor.next();
			historyIdList.add(dbObj.get("roomId"));
		}

		if (historyIdList.isEmpty())
			return null;

//		List<Room> historyList = getRoomDatastore().createQuery(getEntityClass()).field("_id").in(historyIdList).order("-_id").offset(pageIndex * pageSize).limit(pageSize).asList();
		Query<Room> limit = getRoomDatastore().createQuery(getEntityClass()).field("_id").in(historyIdList).field("s").equal(1).order("-_id").offset(pageIndex * pageSize).limit(pageSize);
		List<Room> historyList = limit.asList();
		historyList.forEach(room -> {
			Member member = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(room.getId())
					.field("userId").equal(userId).get();
			room.setMember(member);
		});

		return historyList;
	}

	/* (non-Javadoc)
	 * @see cn.xyz.mianshi.service.RoomManager#deleteMember(cn.xyz.mianshi.vo.User, org.bson.types.ObjectId, int)
	 */
	/* (non-Javadoc)
	 * @see cn.xyz.mianshi.service.RoomManager#deleteMember(cn.xyz.mianshi.vo.User, org.bson.types.ObjectId, int)
	 */
	@Override
	public void deleteMember(User user, ObjectId roomId, int userId) {
		Room room = getRoom(roomId);
		if(-1 == room.getS()){
			throw new ServiceException("????????????????????????");
		}
		Member roomMember = getMember(roomId, user.getUserId());
		Member member = getMember(roomId, userId);
		if(member == null)
			throw new ServiceException("????????????????????????");
		// ?????????????????????
		if(null == roomMember){
			// ???????????????
			Query<Role> roleQuery = SKBeanUtils.getDatastore().createQuery(Role.class).field("userId").equal(user.getUserId());
			if(null != roleQuery.get()){
				if(5 == roleQuery.get().getRole() || 6 == roleQuery.get().getRole()){
					if(-1 == roleQuery.get().getStatus())
						throw new ServiceException("??????????????????????????????");
					if(room.getUserId().equals(userId)){
						throw new ServiceException("??????????????????");
					}
				}
			}else{
				throw new ServiceException("???????????????????????????");
			}
		}else{
			// ????????????
			if(!user.getUserId().equals(userId)){
				if(roomMember.getRole() >= 3)
					throw new ServiceException("??????????????????");
				if(room.getUserId().equals(userId)){
					throw new ServiceException("??????????????????");
				}
				// ?????????????????????????????????????????????????????????
				if(member.getRole() != 1 && member.getRole() != 3){
					// ??????????????????????????????????????????
					if(2 == roomMember.getRole())
						throw new ServiceException("?????????????????????"+(2 == member.getRole() ? "?????????" : 4 == member.getRole() ? "?????????" : "?????????"));
				}
			}
		}
		// ??????????????????
		if(room.getUserId().equals(userId)){
			delete(roomId, userId);
			return;
		}
		User toUser = getUserManager().getUser(userId);
		// IMPORTANT 1-4?????????????????????-??????
		MessageBean messageBean = new MessageBean();
		messageBean.setFromUserId(user.getUserId() + "");
		messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
		messageBean.setType(KXMPPServiceImpl.DELETE_MEMBER);
		// messageBean.setObjectId(roomId.toString());
		messageBean.setObjectId(room.getJid());
		messageBean.setToUserId(userId + "");
		messageBean.setToUserName(toUser.getNickname());
		messageBean.setContent(room.getName());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ????????????????????????
		if(1 == room.getIsAttritionNotice())
			// ??????????????????????????????????????????
			sendChatToOneGroupMsg(userId, room.getJid(), messageBean);
		else
			sendGroupOne(userId, messageBean);
		Query<Room.Member> q = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("userId")
				.equal(userId);
		getRoomDatastore().delete(q);

		updateUserSize(roomId, -1);
		// ???????????????????????????jids
		saveJidsByUserId(userId, queryUserRoomsJidList(userId));
		/**
		 * ?????? ????????????????????? jid  ??????
		 */
		SKBeanUtils.getRedisService().deleteUserRoomJidList(userId);
		SKBeanUtils.getRedisService().removeRoomPushMember(room.getJid(), userId);
		/**
		 * ???????????????????????? ??????
		 */
		updateRoomInfoByRedis(roomId.toString());
		// ????????????????????????????????????
		updateOfflineOperation(user.getUserId(), roomId);
	}

	@Override
	public void updateMember(User user, ObjectId roomId, List<Integer> userIdList) {
		Room room = get(roomId);
		Member invitationMember = getMember(roomId, user.getUserId());
		if(null != invitationMember && 4 == invitationMember.getRole())
			throw new ServiceException("????????????????????????????????????");
		if(room.getMaxUserSize() <room.getUserSize()+userIdList.size())
			throw new ServiceException("?????????????????????  ?????????????????????  "+(room.getMaxUserSize()-room.getUserSize())+"???") ;
		List<Member> list=new ArrayList<>();
		for (int userId : userIdList) {
			User _user = getUserManager().getUser(userId);
			if(null==_user) 
				continue;
			Member _member = new Member();
			if(0<findMemberAndRole(roomId, userId)) {
				logger.info(" ??????   {}   ??????????????????   ",userId);
				continue;
			}
			
			_member.setUserId(userId);
			_member.setRole(3);
			_member.setActive(DateUtil.currentTimeSeconds());
			_member.setCreateTime(_member.getActive());
			_member.setModifyTime(0L);
			_member.setNickname(getUserManager().getNickName(userId));
			_member.setRoomId(roomId);
			_member.setSub(1);
			_member.setTalkTime(0L);
			list.add(_member);
			
			
		}
		
		getRoomDatastore().save(list);
		
		updateUserSize(roomId, list.size());

		list.stream().forEach(member ->{
			// IMPORTANT 1-7???????????????
				MessageBean messageBean = new MessageBean();
				messageBean.setType(KXMPPServiceImpl.NEW_MEMBER);
				// messageBean.setObjectId(roomId.toString());
				messageBean.setObjectId(room.getJid());
				messageBean.setFromUserId(user.getUserId() + "");
				messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
				messageBean.setToUserId(member.getUserId()+ "");
				messageBean.setToUserName(member.getNickname());
				
				messageBean.setFileSize(room.getShowRead());
				messageBean.setContent(room.getName());
				messageBean.setFileName(room.getId().toString());
				
				JSONObject jsonObject=new JSONObject();
				jsonObject.put("showRead", room.getShowRead());
				jsonObject.put("lsLook", room.getIsLook());
				jsonObject.put("isNeedVerify", room.getIsNeedVerify());
				jsonObject.put("showMember", room.getShowMember()); 
				jsonObject.put("allowSendCard", room.getAllowSendCard());
				jsonObject.put("maxUserSize", room.getMaxUserSize());
				messageBean.setOther(jsonObject.toJSONString());
				messageBean.setMessageId(StringUtil.randomUUID());
				
				// ???????????????????????????????????? ??????
				sendChatToOneGroupMsg(member.getUserId(), room.getJid(), messageBean);
				// ????????????????????????jids
				saveJidsByUserId(member.getUserId(), queryUserRoomsJidList(member.getUserId()));
				/**
				 * ?????? ????????????????????? jid  ??????
				 */
				SKBeanUtils.getRedisService().deleteUserRoomJidList(member.getUserId());
				if(0==SKBeanUtils.getUserManager().getOnlinestateByUserId(member.getUserId())) {
					SKBeanUtils.getRedisService().addRoomPushMember(room.getJid(),member.getUserId());
				}
		});
	
	
		/**
		 * ??????????????????????????????
		 */
		updateRoomInfoByRedis(roomId.toString());
		// ????????????????????????????????????
		updateOfflineOperation(user.getUserId(), roomId);
	}
	
	@Override
	public void updateMember(User user, ObjectId roomId, Member member) {
		DBCollection dbCollection = getRoomDatastore().getCollection(Room.Member.class);
		DBObject q = new BasicDBObject().append("roomId", roomId).append("userId", member.getUserId());
		Room room = getRoom(roomId);
		if(null != room && room.getS() == -1)
			throw new ServiceException("???????????????????????????");
		Member oldMember = getMember(roomId, member.getUserId());
		if(null==oldMember) { 
			throw new ServiceException("???????????????????????????"); 
		}
		User toUser = getUserManager().getUser(member.getUserId());
		
		if (1 == dbCollection.count(q)) {
			BasicDBObject values = new BasicDBObject();
			if (0!= member.getRole()&&1!=member.getRole())
				values.append("role", member.getRole());
			if (null != member.getSub())
				values.append("sub", member.getSub());
			if (null != member.getTalkTime())
				values.append("talkTime", member.getTalkTime());
			if (!StringUtil.isEmpty(member.getNickname()))
				values.append("nickname", member.getNickname());
			if (!StringUtil.isEmpty(member.getRemarkName()))
				values.append("remarkName", member.getRemarkName());
			values.append("modifyTime", DateUtil.currentTimeSeconds());
			values.append("call", room.getCall());
			values.append("videoMeetingNo", room.getVideoMeetingNo());
			
			// ??????????????????
			dbCollection.update(q, new BasicDBObject("$set", values));

			if (!StringUtil.isEmpty(member.getNickname()) && !oldMember.getNickname().equals(member.getNickname())) {
				// IMPORTANT 1-1??????????????????-??????
				MessageBean messageBean = new MessageBean();
				messageBean.setType(KXMPPServiceImpl.CHANGE_NICK_NAME);
				// messageBean.setObjectId(roomId.toString());
				messageBean.setObjectId(room.getJid());
				messageBean.setFromUserId(user.getUserId() + "");
				messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
				messageBean.setToUserId(toUser.getUserId() + "");
				messageBean.setToUserName(oldMember.getNickname());
				messageBean.setContent(member.getNickname());
				messageBean.setMessageId(StringUtil.randomUUID());
				// ????????????
				sendGroupMsg(room.getJid(), messageBean);
			}
			if (null != member.getTalkTime()) {
				// IMPORTANT 1-6?????????
				MessageBean messageBean = new MessageBean();
				messageBean.setType(KXMPPServiceImpl.GAG);
				// messageBean.setObjectId(roomId.toString());
				messageBean.setObjectId(room.getJid());
				messageBean.setFromUserId(user.getUserId() + "");
				messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
				messageBean.setToUserId(toUser.getUserId() + "");
				messageBean.setToUserName(oldMember.getNickname());
				messageBean.setContent(member.getTalkTime() + "");
				messageBean.setMessageId(StringUtil.randomUUID());
				// ?????????????????????????????????,??????
				sendChatToOneGroupMsg(toUser.getUserId(), room.getJid(), messageBean);
			}
		} else {
			Member invitationMember = getMember(roomId, user.getUserId());
			if(null != invitationMember && 4 == invitationMember.getRole())
				throw new ServiceException("????????????????????????????????????");
			if(room.getMaxUserSize() < room.getUserSize()+1)
				throw new ServiceException("?????????????????????????????????????????????");
			User _user = getUserManager().getUser(member.getUserId());
			Member _member = new Member(roomId,_user.getUserId(),_user.getNickname());
			getRoomDatastore().save(_member);
			
			updateUserSize(roomId, 1);

			// IMPORTANT 1-7???????????????
			MessageBean messageBean = new MessageBean();
			messageBean.setType(KXMPPServiceImpl.NEW_MEMBER);
			messageBean.setObjectId(room.getJid());
			messageBean.setFromUserId(user.getUserId() + "");
			messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
			messageBean.setToUserId(toUser.getUserId() + "");
			messageBean.setToUserName(toUser.getNickname());
			
			
			messageBean.setFileSize(room.getShowRead());
			messageBean.setContent(room.getName());
			messageBean.setFileName(room.getId().toString());
			
			JSONObject jsonObject=new JSONObject();
			jsonObject.put("showRead", room.getShowRead());
			jsonObject.put("lsLook", room.getIsLook());
			jsonObject.put("isNeedVerify", room.getIsNeedVerify());
			jsonObject.put("showMember", room.getShowMember()); 
			jsonObject.put("allowSendCard", room.getAllowSendCard());
			jsonObject.put("maxUserSize", room.getMaxUserSize());
			messageBean.setOther(jsonObject.toJSONString());
			messageBean.setMessageId(StringUtil.randomUUID());
			
			// ???????????????????????????????????? ??????
			sendChatToOneGroupMsg(toUser.getUserId(), room.getJid(), messageBean);
			// ????????????????????????jids
			saveJidsByUserId(toUser.getUserId(), queryUserRoomsJidList(toUser.getUserId()));
			/**
			 * ?????? ????????????????????? jid  ??????
			 */
			SKBeanUtils.getRedisService().deleteUserRoomJidList(member.getUserId());
			if(0==SKBeanUtils.getUserManager().getOnlinestateByUserId(member.getUserId())) {
				SKBeanUtils.getRedisService().addRoomPushMember(room.getJid(), member.getUserId());
			}
		}
		
		/**
		 * ??????????????????????????????
		 */
		updateRoomInfoByRedis(roomId.toString());
		// ????????????????????????????????????
		updateOfflineOperation(user.getUserId(), roomId);
	}

	@Override
	public Member getMember(ObjectId roomId, int userId) {

		return getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("userId").equal(userId).get();
		
	}
	public int findMemberAndRole(ObjectId roomId, int userId) {
		Object role= queryOneField("shiku_room_member", "role", 
				new BasicDBObject("roomId", roomId).append("userId", userId));
		return null!=role?(int)role:0;
	}
	
	
	
	@Override
	public void Memberset(Integer offlineNoPushMsg, ObjectId roomId,int userId,int type) {
		Query<Room.Member> q = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("userId")
				.equal(userId);
		UpdateOperations<Room.Member> ops = getDatastore().createUpdateOperations(Room.Member.class);
		 long currentTime = DateUtil.currentTimeSeconds();
		if(0 == type){
			ops.set("offlineNoPushMsg",offlineNoPushMsg);
			String jid = queryRoomJid(roomId);
			if(1==offlineNoPushMsg) {
				SKBeanUtils.getRedisService().addToRoomNOPushJids(userId, jid);
			}else {
				SKBeanUtils.getRedisService().removeToRoomNOPushJids(userId, jid);
			}
		}else if(1 == type){ 
			ops.set("openTopChatTime", (offlineNoPushMsg == 0 ? 0 : currentTime));
		}
		ops.set("modifyTime", currentTime);
		getRoomDatastore().update(q, ops);
		// ????????????????????????????????????
		updateRoomInfoByRedis(roomId.toString());
		// ????????????????????????
		if(getUserManager().isOpenMultipleDevices(userId)){
			String nickName = getUserManager().getNickName(userId);
			multipointLoginUpdateUserInfo(userId, nickName, userId, nickName, roomId);
		}
	}

	@Override
	public List<Member> getMemberList(ObjectId roomId,String keyword) {
		List<Member>list=null;
		if(!StringUtil.isEmpty(keyword)){
			Query<Member> query = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId);
			query.field("nickname").containsIgnoreCase(keyword);
			list=query.order("createTime").order("role").asList();
		}else{
			List<Member> memberList = getRedisServiceImpl().getMemberList(roomId.toString());
			if(null != memberList && memberList.size() > 0){
				list = memberList;
			}else{
				List<Member> memberDBList = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).asList();
				list = memberDBList;
//				getRedisServiceImpl().saveMemberList(roomId.toString(), memberDBList);
			}
		}
		return list;
	}
	
	/** @Description:????????????????????????????????????
	* @param roomId
	* @return
	**/ 
	public List<Member> getAdministrationMemberList(ObjectId roomId) {
		List<Member> members = null;
		// ?????????
		List<Member> memberList = getRedisServiceImpl().getMemberList(roomId.toString());
		if(null != memberList && memberList.size() > 0){
			List<Member> adminMembers = new ArrayList<Member>();// ??????????????????
			for (Member member : memberList) {
				if(member.getRole() == 1 || member.getRole() == 2){
					adminMembers.add(member);
				}
				members = adminMembers;
			}
		} else{
			Query<Member> query = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).order("createTime");
			List<Member> membersList = query.asList();
			List<Member> memberPageList = query.field("role").lessThanOrEq(2).asList();
			members = memberPageList;
			/**
			 * ???????????????????????????
			 */
//			getRedisServiceImpl().saveMemberList(roomId.toString(), membersList);
		}
		return members;
	}
	
	/** @Description: ???????????????userId?????????????????????????????????
	* @param roomId
	* @return
	**/ 
	@SuppressWarnings("unchecked")
	public List<Integer> getCommonMemberIdList(ObjectId roomId) {
		List<Integer> members =distinct("shiku_room_member","userId",new BasicDBObject("roomId", roomId).append("role",3));
		return members;
	}
	/** @Description: ?????????userId??????
	* @param roomId
	* @return
	**/ 
	@SuppressWarnings("unchecked")
	public List<Integer> getMemberIdList(ObjectId roomId) {
		List<Integer> members =distinct("shiku_room_member","userId",new BasicDBObject("roomId", roomId));
		return members;
	}
	
	@SuppressWarnings("unchecked")
	public List<ObjectId> getRoomIdList(Integer userId) {
		List<ObjectId> roomIds =distinct("shiku_room_member","roomId",new BasicDBObject("userId", userId));
		return roomIds;
	}
	
	/**
	 * ???????????????????????? ?????????
	 * @param roomId
	 * @param userId
	 * @return
	 */
	public boolean getMemberIsNoPushMsg(ObjectId roomId, int userId) {
		DBObject query=new BasicDBObject("roomId", roomId).append("userId", userId);
		query.put("offlineNoPushMsg", 1);
		Object field = queryOneField("shiku_room_member","offlineNoPushMsg",query);
		return null!=field;
	}
	
	public String getMemberNickname(ObjectId roomId,Integer userId){
		String nickname = null;
		Query<Member> query = getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId);
		if(query.asList().size() == 0)
			throw new ServiceException("???????????????");
		if(0 != userId){
			query.field("userId").equal(userId);
			if(null == query.get()){
				// ???????????????
				Query<Role> roleQuery = SKBeanUtils.getDatastore().createQuery(Role.class).field("userId").equal(userId);
				if(null != roleQuery.get()){
					if(5 == roleQuery.get().getRole() || 6 == roleQuery.get().getRole()){
						if(1 == roleQuery.get().getStatus())
							nickname = "???????????????";// ??????????????????????????????
						else
							throw new ServiceException("?????????????????????????????????");
					}
				}else{
					throw new ServiceException("???????????????????????????");
				}
			}else
				nickname = query.get().getNickname();
		}
		return nickname;
	}
	
	/*????????????*/
	public List<Notice> getNoticeList(ObjectId roomId){
		List<Notice> notices;
		List<Notice> noticeList = getRedisServiceImpl().getNoticeList(roomId);
		if(null != noticeList && noticeList.size() > 0)
			notices = noticeList;
		else{
			List<Notice> noticesDB = getRoomDatastore().createQuery(Room.Notice.class).field("roomId").equal(roomId).asList();
			notices = noticesDB;
		}
		return notices;
	}
	
	/*????????????*/
	public PageVO getNoticeList(ObjectId roomId,Integer pageIndex,Integer pageSize){
		
		Query<Notice> query = getRoomDatastore().createQuery(Room.Notice.class).field("roomId").equal(roomId).order("-time");
		long total = query.count();
		List<Notice> pageData = query.offset(pageIndex * pageSize).limit(pageSize).asList();
		return new PageVO(pageData, total, pageIndex, pageSize);
	}
	
	public Notice updateNotice(ObjectId roomId,ObjectId noticeId,String noticeContent,Integer userId){
		Query<Notice> query = getRoomDatastore().createQuery(Room.Notice.class).field("roomId").equal(roomId).field("_id").equal(noticeId);
		UpdateOperations<Notice> ops = SKBeanUtils.getDatastore().createUpdateOperations(Notice.class);
		ops.set("text", noticeContent);
		ops.set("modifyTime", DateUtil.currentTimeSeconds());
		getDatastore().update(query, ops);
		Notice notice = query.get();
		// ????????????????????????
		Room room = getRoom(roomId);
		if(room.getNotice().getId().equals(noticeId)){
			getRedisServiceImpl().deleteRoom(String.valueOf(roomId));
			updateAttribute(roomId, "notice", notice);
		}
		getRedisServiceImpl().deleteNoticeList(roomId);
		ThreadUtil.executeInThread(new Callback() {
			
			@Override
			public void execute(Object obj) {
				MessageBean messageBean = new MessageBean();
				messageBean.setFromUserId(userId + "");
				messageBean.setFromUserName(getMemberNickname(room.getId(), userId));
				messageBean.setType(KXMPPServiceImpl.ModifyNotice);
				messageBean.setObjectId(room.getJid());
				messageBean.setContent(noticeContent);
				messageBean.setMessageId(StringUtil.randomUUID());
				// ????????????
				sendGroupMsg(room.getJid(), messageBean);
			}
		});
		return notice;
	}
	
	public void deleteNotice(ObjectId roomId, ObjectId noticeId) {
		Query<Notice> query = getRoomDatastore().createQuery(Notice.class);
		query.filter("_id", noticeId);
		query.filter("roomId", roomId);
		getRoomDatastore().delete(query);
		// ??????room????????????
		Room room = getRoom(roomId);
		if (null != room.getNotice() && noticeId.equals(room.getNotice().getId())) {
			updateAttribute(roomId, "notice", new Notice());
		}
		/**
		 * ?????????????????? ???????????????
		 */
		getRedisServiceImpl().deleteNoticeList(roomId);
		getRedisServiceImpl().deleteRoom(roomId.toString());
	}
	
	
	public PageResult<Member> getMemberListByPage(ObjectId roomId,int pageIndex,int pageSize) {
		Query<Room.Member> q=getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).order("-createTime");
		//??????
		List<Member> pageData=q.asList(pageFindOption(pageIndex, pageSize, 1)); 
		return new PageResult<Member>(pageData,q.count());
	}
	

	@Override
	public void join(int userId, ObjectId roomId, int type) {
		Room room = getRoom(roomId);
		if(room != null){
			if(room.getUserSize()+1>room.getMaxUserSize()){
				throw new ServiceException("?????????????????????????????????????????????");
			}
		}else{
			throw new ServiceException("???????????????");
		}
		Member member = new Member();
		member.setUserId(userId);
		member.setRole(1 == type ? 1 : 3);
		sweepCode(roomId,getUserManager().getUser(userId), member);
//		updateMember(getUserManager().getUser(userId), roomId, member);
	}
	
	// ????????????
	public void sweepCode(ObjectId roomId,User user,Member member){
		Room room = getRoom(roomId);
		if(null != room && room.getS() == -1)
			throw new ServiceException("???????????????????????????");
		User toUser = getUserManager().getUser(member.getUserId());
		if(room.getMaxUserSize() < room.getUserSize()+1)
			throw new ServiceException("?????????????????????????????????????????????");
		User memberUser = getUserManager().getUser(member.getUserId());
		Member _member = new Member(roomId,memberUser.getUserId(),memberUser.getNickname());
		getRoomDatastore().save(_member);
		updateUserSize(roomId, 1);
		MessageBean messageBean = new MessageBean();
		messageBean.setType(KXMPPServiceImpl.NEW_MEMBER);
		messageBean.setObjectId(room.getJid());
		messageBean.setFromUserId(user.getUserId() + "");
		messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
		messageBean.setToUserId(toUser.getUserId() + "");
		messageBean.setToUserName(toUser.getNickname());
		messageBean.setFileSize(room.getShowRead());
		messageBean.setContent(room.getName());
		messageBean.setFileName(room.getId().toString());
		
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("showRead", room.getShowRead());
		jsonObject.put("lsLook", room.getIsLook());
		jsonObject.put("isNeedVerify", room.getIsNeedVerify());
		jsonObject.put("showMember", room.getShowMember()); 
		jsonObject.put("allowSendCard", room.getAllowSendCard());
		jsonObject.put("maxUserSize", room.getMaxUserSize());
		messageBean.setOther(jsonObject.toJSONString());
		messageBean.setMessageId(StringUtil.randomUUID());
		
		// ???????????????????????????????????? ??????
		sendChatToOneGroupMsg(toUser.getUserId(), room.getJid(), messageBean);
		// ????????????????????????jids
		saveJidsByUserId(toUser.getUserId(), queryUserRoomsJidList(toUser.getUserId()));
		/**
		 * ?????? ????????????????????? jid  ??????
		 */
		SKBeanUtils.getRedisService().deleteUserRoomJidList(member.getUserId());
		if(0==SKBeanUtils.getUserManager().getOnlinestateByUserId(member.getUserId())) {
			SKBeanUtils.getRedisService().addRoomPushMember(room.getJid(), member.getUserId());
		}
		/**
		 * ??????????????????????????????
		 */
		updateRoomInfoByRedis(roomId.toString());
		// ????????????????????????????????????
		updateOfflineOperation(user.getUserId(), roomId);
	}
	
	public void joinRoom(Integer userId,String name,ObjectId roomId, int type) {
		Room room = getRoom(roomId);
		if(room == null){
			throw new ServiceException("???????????????");
		}
		List<Member> memberList=Collections.synchronizedList(new ArrayList<Member>());
		List<MessageBean> messageList=Collections.synchronizedList(new ArrayList<MessageBean>());
		JSONObject jsonObject=new JSONObject();
		jsonObject.put("showRead", room.getShowRead());
		jsonObject.put("lsLook", room.getIsLook());
		jsonObject.put("isNeedVerify", room.getIsNeedVerify());
		jsonObject.put("showMember", room.getShowMember());
		jsonObject.put("allowSendCard", room.getAllowSendCard());
		jsonObject.put("maxUserSize", room.getMaxUserSize());
		
		Member member = new Member();
		member.setUserId(userId);
		member.setRole(1 == type ? 1 : 3);
		member.setNickname(name);
		member.setCreateTime(DateUtil.currentTimeSeconds());
		member.setRoomId(roomId);
		memberList.add(member);
		getDatastore().save(member);
		MessageBean messageBean = new MessageBean();
		messageBean.setType(KXMPPServiceImpl.NEW_MEMBER);
		messageBean.setObjectId(room.getJid());
		messageBean.setFromUserId(userId + "");
		messageBean.setFromUserName(member.getNickname());
		messageBean.setToUserId(userId+"");
		messageBean.setToUserName(member.getNickname());
		messageBean.setFileSize(room.getShowRead());
		messageBean.setContent(room.getName());
		messageBean.setFileName(room.getId().toString());
		messageBean.setOther(jsonObject.toJSONString());
		messageBean.setMessageId(StringUtil.randomUUID());
		
		messageList.add(messageBean);
		updateUserSize(room.getId(), 1);
		/**
		 * ??????????????????????????????
		 */
		updateRoomInfoByRedis(roomId.toString());
		
		KXMPPServiceImpl.getInstance().sendManyMsgToGroupByJid(room.getJid(), messageList);
	}

	private void updateUserSize(ObjectId roomId, int userSize) {
		DBObject q = new BasicDBObject("_id", roomId);
		DBObject o = new BasicDBObject("$inc", new BasicDBObject("userSize", userSize));
		getRoomDatastore().getCollection(getEntityClass()).update(q, o);
	}

	@Override
	public Room exisname(Object roomname,ObjectId roomId) {
		Query<Room> query = getRoomDatastore().createQuery(getEntityClass());
		query.field("name").equal(roomname);
		if(null!=roomId)
			query.field("_id").notEqual(roomId);
		Room room = query.get();
		return room;
	}

	
	/**
	* @Description: TODO(?????? ?????????????????? ??? ????????????????????????)
	* @param @param roomId
	* @param @param roomJid    ??????
	 */
	public void destroyRoomMsgFileAndShare(ObjectId roomId,String roomJid){
		//?????????????????? 
		Query<Share> shareQuery = getRoomDatastore().createQuery(Room.Share.class).field("roomId").equal(roomId);
		List<String> shareList = getRoomDatastore().getCollection(Share.class).distinct("url", shareQuery.getQueryObject());
		for (String url : shareList) {
			try {
				ConstantUtil.deleteFile(url);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		getRoomDatastore().delete(shareQuery);
		
		BasicDBObject msgFileQuery=new BasicDBObject("contentType",new BasicDBObject(MongoOperator.IN, MsgType.FileTypeArr));
		List<String> fileList = getRoomDatastore().getDB().getCollection(mucMsg+roomJid).distinct("content", msgFileQuery);
		for (String url : fileList) {
			try {
				ConstantUtil.deleteFile(url);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		getRoomDatastore().getDB().getCollection(mucMsg+roomJid).drop();
	}
	
	/**
	*@Description: TODO(???????????????  ?????? tigase ?????????)
	* @param @param roomJid    ??????
	* tigase 8.0 ????????? ??? ??????
	 */
	@Deprecated()
	public void destroyRoomToIM(String roomJid){
		/*?????? tig_nodes ???????????????*/
		DBCollection collection = getTigaseDatastore().getDB().getCollection("tig_nodes");
		String queryJid="rooms/"+roomJid;
		Pattern regex = Pattern.compile("^" + (queryJid != null ? queryJid : "") + "[^/]*");
		BasicDBObject query = new BasicDBObject("node", regex);
		collection.remove(query);
	}
	/**
	* @Description: TODO(?????? ??????xmpp ????????????)
	* @param @param username ?????????userid
	* @param @param password  ???????????????
	* @param @param roomJid     ??????jid
	 */
	public void destroyRoomToIM(String username,String password,String roomJid){
		KXMPPServiceImpl.getInstance().destroyMucRoom(username, password, roomJid);
	}
	
	/** @Description:???????????????????????????????????????????????? 
	* @param roomJid
	**/ 
	public void deleMucHistory(String roomJid){
		DBCollection collection =getDatastore().getDB().getCollection("muc_history");
		Pattern regex = Pattern.compile("^" + (roomJid != null ? roomJid : "") + "[^/]*");
		BasicDBObject query = new BasicDBObject("room_jid", regex);
		collection.remove(query);
	}
	//??????/???????????????
	@Override
	public void setAdmin(ObjectId roomId, int touserId,int type,int userId) {
		Integer status = queryRoomStatus(roomId);
		if(null != status && status == -1)
			throw new ServiceException("???????????????????????????");
		Query<Room.Member> q=getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("userId").equal(touserId);
		if(null == q.get())
			throw new ServiceException("????????????????????????");
		UpdateOperations<Room.Member> ops=getRoomDatastore().createUpdateOperations(Room.Member.class);
		ops.set("role", type);
		getRoomDatastore().findAndModify(q, ops);
		// ????????????????????????????????????
		updateRoomInfoByRedis(roomId.toString());
		Room room=getRoom(roomId);
		User user = getUserManager().getUser(userId);
		//xmpp??????
		MessageBean messageBean=new MessageBean();
		messageBean.setType(KXMPPServiceImpl.SETADMIN);
		if(type==2){//1??????????????????
			messageBean.setContent(1);
		}else{
			messageBean.setContent(0);
		}
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
		messageBean.setToUserName(q.get().getNickname());
		messageBean.setToUserId(q.get().getUserId().toString());
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ??????????????????????????????????????????
		sendChatToOneGroupMsg(q.get().getUserId(), room.getJid(), messageBean);
	}
	
	public void setInvisibleGuardian(ObjectId roomId, int touserId,int type,int userId) {
		Query<Room.Member> q=getRoomDatastore().createQuery(Room.Member.class).field("roomId").equal(roomId).field("userId").equal(touserId);
		UpdateOperations<Room.Member> ops=getRoomDatastore().createUpdateOperations(Room.Member.class);
		if(type == -1 || type == 0)
			ops.set("role", 3);// 1=????????????2=????????????3=???????????????4=????????????5=?????????
		else if(type == 4 || type == 5){
			ops.set("role", type);
		}
		getRoomDatastore().findAndModify(q, ops);
		/**
		 * ????????????????????????????????????
		 */
		updateRoomInfoByRedis(roomId.toString());
		Room room=getRoom(roomId);
		//xmpp??????
		MessageBean messageBean=new MessageBean();
		messageBean.setType(KXMPPServiceImpl.SetRoomSettingInvisibleGuardian);
		if(type==4){
			messageBean.setContent(1);
		}else if(type==5){
			messageBean.setContent(2);
		}else if(type == -1){
			messageBean.setContent(-1);
		}else if(type == 0){
			messageBean.setContent(0);
		}
		messageBean.setFromUserId(String.valueOf(userId));
		messageBean.setFromUserName(getMemberNickname(roomId, userId));
		messageBean.setToUserName(q.get().getNickname());
		messageBean.setToUserId(String.valueOf(touserId));
		messageBean.setObjectId(room.getJid());
		messageBean.setMessageId(StringUtil.randomUUID());
		// ??????????????????????????????????????????
//		sendChatToOneGroupMsg(q.get().getUserId(), room.getJid(), messageBean);
		sendGroupOne(q.get().getUserId(), messageBean);
	}
	
	//???????????????????????????
	@Override
	public Share Addshare(ObjectId roomId,long size, int type,int userId, String url,String name) {
		User user = getUserManager().getUser(userId);
		Share share=new Share();
		share.setRoomId(roomId);
		share.setTime(DateUtil.currentTimeSeconds());
		share.setNickname(user.getNickname());
		share.setUserId(userId);
		share.setSize(size);
		share.setUrl(url);
		share.setType(type);
		share.setName(name);
		getRoomDatastore().save(share);
		/**
		 * ?????????????????????
		 */
		getRedisServiceImpl().deleteShareList(roomId);
		Room room=getRoom(roomId);
		//????????????xmpp??????
		MessageBean messageBean=new MessageBean();
		messageBean.setType(KXMPPServiceImpl.FILEUPLOAD);
		messageBean.setContent(share.getShareId().toString());
		messageBean.setFileName(share.getName());
		messageBean.setObjectId(room.getJid());
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
		messageBean.setMessageId(StringUtil.randomUUID());
		// ??????????????????
		sendGroupMsg(room.getJid(), messageBean);
		return share;
	}
	
	//????????????
	@SuppressWarnings("deprecation")
	@Override
	public List<Share> findShare(ObjectId roomId, long time, int userId, int pageIndex, int pageSize) {
		if (userId != 0) {
			Query<Room.Share> q = getRoomDatastore().createQuery(Room.Share.class).field("roomId").equal(roomId).order("-time");
			q.filter("userId", userId);
			return q.offset(pageSize * pageIndex).limit(pageSize).asList();
		}else{
			List<Share> shareList;
			List<Share> redisShareList = getRedisServiceImpl().getShareList(roomId, pageIndex, pageSize);
			if(null != redisShareList && redisShareList.size() > 0){
				shareList = redisShareList;
			}else{
				Query<Room.Share> q = getRoomDatastore().createQuery(Room.Share.class).field("roomId").equal(roomId).order("-time");
				getRedisServiceImpl().saveShareList(roomId, q.asList());
				shareList = q.offset(pageSize * pageIndex).limit(pageSize).asList();
			}
			return shareList;
		}
	}
	
	//??????
	@Override
	public void deleteShare(ObjectId roomId, ObjectId shareId,int userId) {
		Query<Room.Share> q=getRoomDatastore().createQuery(Room.Share.class).field("roomId").equal(roomId).field("shareId").equal(shareId);
		
		User user = getUserManager().getUser(userId);
		Room room=getRoom(roomId);
		Share share=q.get();
		//??????XMpp??????
		MessageBean messageBean=new MessageBean();
		messageBean.setType(KXMPPServiceImpl.DELETEFILE);
		messageBean.setContent(share.getShareId().toString());
		messageBean.setFileName(share.getName());
		messageBean.setObjectId(room.getJid());
		messageBean.setFromUserId(user.getUserId().toString());
		messageBean.setFromUserName(getMemberNickname(roomId, user.getUserId()));
		messageBean.setMessageId(StringUtil.randomUUID());
		// ??????????????????
		sendGroupMsg(room.getJid(), messageBean);
		getRoomDatastore().delete(q);
		/**
		 * ?????????????????????
		 */
		getRedisServiceImpl().deleteShareList(roomId);
	}
	//??????????????????
	@Override
	public Object getShare(ObjectId roomId, ObjectId shareId) {
		Share share=getRoomDatastore().createQuery(Room.Share.class).field("roomId").equal(roomId).field("shareId").equal(shareId).get();
		return share;
	}

	@Override
	public String getCall(ObjectId roomId) {
		Room room = getRoomDatastore().createQuery(getEntityClass()).field("_id").equal(roomId).get();
		return room.getCall();
	}

	@Override
	public String getVideoMeetingNo(ObjectId roomId) {
		Room room=getRoomDatastore().createQuery(getEntityClass()).field("_id").equal(roomId).get();
		return room.getVideoMeetingNo();
	}
	
	/**
	 * ???????????? ????????????
	 * @param jidArr
	 * @param userId
	 * @param msgType
	 * @param content
	 */
	public void sendMsgToRooms(String[] jidArr, int userId,int msgType,String content) {
		this.sendMsgToRooms(jidArr, userId, msgType, content, null, 0);
	}

	/**
	 * ???????????? ????????????
	 * @param jidArr
	 * @param userId
	 * @param msgType
	 * @param content
	 */
	public void sendMsgToRooms(String[] jidArr, int userId,int msgType,String content, Object objectId, int fileSize) {
		User user = SKBeanUtils.getUserManager().getUser(userId);
		MessageBean messageBean=new MessageBean();
		messageBean.setFromUserId(userId+"");
		messageBean.setFromUserName(user.getNickname());
		messageBean.setType(msgType);
		messageBean.setTimeSend(DateUtil.currentTimeSeconds());
		messageBean.setContent(content);
		messageBean.setMessageId(StringUtil.randomUUID());
		messageBean.setObjectId(objectId);
		messageBean.setFileSize(fileSize);
		SKBeanUtils.getXmppService().sendMsgToMucRoom(messageBean, jidArr);
	}
	
	/**
	 * ?????????????????????
	 */
	@Override
    public Long countRoomNum(){
    	long roomNum = getRoomDatastore().createQuery(getEntityClass()).count();
    	return roomNum;
    }
	
	
	/**
	 * ??????????????????      ??????????????????????????????????????????????????????????????????????????????
	 * @param startDate
	 * @param endDate
	 * @param counType  ????????????   1: ??????????????????      2:???????????????       3.???????????????   4.?????????????????? (??????)   
	 */
	public List<Object> addRoomsCount(String startDate, String endDate, short counType){
		
		List<Object> countData = new ArrayList<>();
		
		long startTime = 0; //?????????????????????
		
		long endTime = 0; //?????????????????????,?????????????????????
		
		/**
		 * ??????????????????????????????????????????????????????????????????????????? ; ????????????????????????????????????????????????????????????????????????;
		 * ??????????????????????????????????????????????????????????????????0???
		 */
		long defStartTime = counType==4? DateUtil.getTodayMorning().getTime()/1000 
				: counType==3 ? DateUtil.getLastMonth().getTime()/1000 : DateUtil.getLastYear().getTime()/1000;
		
		startTime = StringUtil.isEmpty(startDate) ? defStartTime :DateUtil.toDate(startDate).getTime()/1000;
		endTime = StringUtil.isEmpty(endDate) ? DateUtil.currentTimeSeconds() : DateUtil.toDate(endDate).getTime()/1000;
				
		BasicDBObject queryTime = new BasicDBObject("$ne",null);
		
		if(startTime!=0 && endTime!=0){
			queryTime.append("$gt", startTime);
			queryTime.append("$lt", endTime);
		}
		
		BasicDBObject query = new BasicDBObject("createTime",queryTime);
		
		//????????????????????????
		DBCollection collection = SKBeanUtils.getImRoomDatastore().getCollection(getEntityClass());
		
		String mapStr = "function Map() { "   
	            + "var date = new Date(this.createTime*1000);" 
				+  "var year = date.getFullYear();"
				+  "var month = (\"0\" + (date.getMonth()+1)).slice(-2);"  //month ???0?????????????????????1
				+  "var day = (\"0\" + date.getDate()).slice(-2);"
				+  "var hour = (\"0\" + date.getHours()).slice(-2);"
				+  "var minute = (\"0\" + date.getMinutes()).slice(-2);"
				+  "var dateStr = date.getFullYear()"+"+'-'+"+"(parseInt(date.getMonth())+1)"+"+'-'+"+"date.getDate();";
				
				if(counType==1){ // counType=1: ??????????????????
					mapStr += "var key= year + '-'+ month;";
				}else if(counType==2){ // counType=2:???????????????
					mapStr += "var key= year + '-'+ month + '-' + day;";
				}else if(counType==3){ //counType=3 :???????????????
					mapStr += "var key= year + '-'+ month + '-' + day + '  ' + hour +' : 00';";
				}else if(counType==4){ //counType=4 :??????????????????
					mapStr += "var key= year + '-'+ month + '-' + day + '  ' + hour + ':'+ minute;";
				}
	           
				mapStr += "emit(key,1);}";
		
		 String reduce = "function Reduce(key, values) {" +
			                "return Array.sum(values);" +
	                    "}";
		 MapReduceCommand.OutputType type =  MapReduceCommand.OutputType.INLINE;
		 MapReduceCommand command = new MapReduceCommand(collection, mapStr, reduce,null, type,query);


		int i = 0;
		MapReduceOutput mapReduceOutput = null;
		while (i < 5) {
			i++;
			try {
				mapReduceOutput = collection.mapReduce(command);
				break;
			} catch (MongoSocketWriteException e) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				logger.info("retry addRoomsCount mapReduce:{}", i);
			}
		}
		 Iterable<DBObject> results = mapReduceOutput.results();
		 Map<String,Double> map = new HashMap<String,Double>();
		for (Iterator iterator = results.iterator(); iterator.hasNext();) {
			DBObject obj = (DBObject) iterator.next();
			 
			map.put((String)obj.get("_id"),(Double)obj.get("value"));
			countData.add(JSON.toJSON(map));
			map.clear();
			//System.out.println(JSON.toJSON(obj));
			
		}
		
		return countData;
	}
	
	/** @Description: ???????????????
	* @param roomId
	* @param request
	* @param response
	* @return
	**/ 
	public Workbook exprotExcelGroupMembers(String roomId,HttpServletRequest request,HttpServletResponse response) {

		String name = getRoomName(new ObjectId(roomId))+" ??????????????????";
		
		String fileName ="groupMembers.xlsx";

		List<Member> members = getMemberList(new ObjectId(roomId), null);
		List<String> titles = Lists.newArrayList();
		titles.add("userId");
		titles.add("userName");
		titles.add("remarkName");
		titles.add("telephone");
		titles.add("role");
		titles.add("offlineNoPushMsg");
		titles.add("createTime");
		titles.add("modifyTime");

		List<Map<String, Object>> values = Lists.newArrayList();
		members.forEach(member ->{
			Map<String, Object> map = Maps.newHashMap();
			map.put("userId", member.getUserId());
			map.put("userName", member.getNickname());
			map.put("remarkName", member.getRemarkName());
			map.put("telephone", getUserManager().getUser(member.getUserId()).getPhone());
			map.put("role", member.getRole() == 1 ?"??????":member.getRole() == 2 ? "?????????" : member.getRole() == 3?"????????????":member.getRole() == 4 ? "?????????" : "?????????");// 1=????????????2=????????????3=???????????????4=????????????5=?????????
			map.put("offlineNoPushMsg", member.getOfflineNoPushMsg() == 0 ? "???" : "???");
			map.put("createTime", DateUtil.strToDateTime(member.getCreateTime()));
			map.put("modifyTime", DateUtil.strToDateTime(member.getModifyTime()));
			values.add(map);
		});

		Workbook workBook = ExcelUtil.generateWorkbook(name, "xlsx", titles, values);
		response.reset();
		try {
			response.setHeader("Content-Disposition",
					"attachment;filename=" + new String(fileName.getBytes(), "utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return workBook;
	}
	
	
	/** @Description: ????????????????????? userId ====> jids
	* @param userId
	* @param jids
	**/ 
	public void saveJidsByUserId(Integer userId, List<String> jids) {
		if (null == jids)
			return;
		else {
			DBCollection collection = getRoomDatastore().getDB().getCollection(SHIKU_ROOMJIDS_USERID);
			BasicDBObject query = new BasicDBObject("userId", userId);
			if(0 == jids.size()){
				collection.remove(query);
				return;
			}
			BasicDBObject values = new BasicDBObject("jids", jids);
			values.append("jids", jids);
			collection.update(query, new BasicDBObject("$set", values), true, false);
		}
	}

	/** @Description: 
	* @param user
	* @param roomId
	* @param userIdList
	**/ 
	public void consoleJoinRoom(User user, ObjectId roomId, List<Integer> userIdList){
		for (Integer userId : userIdList) {
			Member data = SKBeanUtils.getRoomManagerImplForIM().getMember(roomId, userId);
			if(null != data)
				throw new ServiceException(userId+" ???????????????????????????,??????????????????");
			Member member = new Member();
			member.setActive(DateUtil.currentTimeSeconds());
			member.setCreateTime(DateUtil.currentTimeSeconds());
			member.setModifyTime(0L);
			member.setNickname(getUserManager().getNickName(userId));
			member.setRole(3);
			member.setRoomId(roomId);
			member.setSub(1);
			member.setTalkTime(0L);
			member.setUserId(userId);
			getDatastore().save(member);
			// ????????????
			updateUserSize(roomId, 1);
			Room room = getRoom(roomId);
			// ??????????????????????????????
			MessageBean messageBean = new MessageBean();
			messageBean.setType(KXMPPServiceImpl.NEW_MEMBER);
			messageBean.setObjectId(room.getJid());
			messageBean.setFromUserId(user.getUserId().toString());
			messageBean.setFromUserName(user.getNickname());
			messageBean.setToUserId(userId.toString());
			messageBean.setToUserName(getUserManager().getNickName(userId));
			messageBean.setContent(room.getName());
			messageBean.setMessageId(StringUtil.randomUUID());
			messageBean.setFileSize(room.getShowRead());
			messageBean.setFileName(room.getId().toString());
			// ???????????????????????????????????? ??????
			sendChatToOneGroupMsg(userId, room.getJid(), messageBean);
			getRedisServiceImpl().deleteMemberList(roomId.toString());
			// ????????????????????????jids
			saveJidsByUserId(userId, queryUserRoomsJidList(userId));
			/**
			 * ?????? ????????????????????? jid  ??????
			 */
			SKBeanUtils.getRedisService().deleteUserRoomJidList(member.getUserId());
			if(0==SKBeanUtils.getUserManager().getOnlinestateByUserId(member.getUserId())) {
				SKBeanUtils.getRedisService().addRoomPushMember(room.getJid(), member.getUserId());
			}
			// ??????????????????
			getRedisServiceImpl().deleteRoom(String.valueOf(roomId));
		}
	}
	
	// ???????????????
	public   Room queryLocationRoom(String name,double longitude,double latitude,String password,
			int isQuery){
		Integer userId = ReqUtil.getUserId();
		Room  room=SKBeanUtils.getRedisService().
				queryLocationRoom(userId, longitude, latitude, password, name);
		if(1==isQuery)
			return room;
		ThreadUtil.executeInThread(new Callback() {
			
			@Override
			public void execute(Object obj) {
				for (Member mem : room.getMembers()) {
					if(userId.equals(mem.getUserId()))
						continue;
					MessageBean messageBean = new MessageBean();
					messageBean.setObjectId(room.getJid());
					messageBean.setFromUserId(userId.toString());
					messageBean.setFromUserName(userId.toString());
					messageBean.setType(KXMPPServiceImpl.LocationRoom);
					messageBean.setToUserId(mem.getUserId().toString());
					SKBeanUtils.getXmppService().send(messageBean);
				}
			}
		});
		return room;
	}
	
	public synchronized Room joinLocationRoom(String roomJid) {
		ObjectId roomId = getRoomId(roomJid);
		Integer userId = ReqUtil.getUserId();
		User user=null;
		if(null==roomId) {
			Room room = SKBeanUtils.getRedisService().queryLocationRoom(roomJid);
			if(null==room)
				throw new ServiceException("?????????????????????");
			user=SKBeanUtils.getUserManager().getUser(userId);
			SKBeanUtils.getXmppService().createMucRoom(user.getPassword(), userId.toString(), room.getName(),roomJid, room.getName(), room.getName());
			roomId=new ObjectId();
			room.setId(roomId);
			add(user, room, null);
		
			SKBeanUtils.getRedisService().saveLocationRoom(roomJid,room);
		}else {
			user=SKBeanUtils.getUserManager().getUser(userId);
			Member member=new Member();
			member.setUserId(userId);
			updateMember(user, roomId, member);
		}
		return get(roomId);
	}
	public void exitLocationRoom(String roomJid) {
		Integer userId = ReqUtil.getUserId();
		SKBeanUtils.getRedisService().exitLocationRoom(userId, roomJid);
		ThreadUtil.executeInThread(new Callback() {
			
			@Override
			public void execute(Object obj) {
				Room room = SKBeanUtils.getRedisService().queryLocationRoom(roomJid);
				for (Member mem : room.getMembers()) {
					if(userId.equals(mem.getUserId()))
						continue;
					MessageBean messageBean = new MessageBean();
					messageBean.setObjectId(room.getJid());
					messageBean.setFromUserId(userId.toString());
					messageBean.setFromUserName(userId.toString());
					messageBean.setType(KXMPPServiceImpl.LocationRoom);
					messageBean.setToUserId(mem.getUserId().toString());
					SKBeanUtils.getXmppService().send(messageBean);
				}
			}
		});
	}
	
	/**
	 * ???????????????
	 * @param entity
	 * @return
	 */
	public JSONMessage addGroupHelper(String helperId,String roomId,String roomJid,Integer userId){
		
		Query<GroupHelper> query = SKBeanUtils.getDatastore().createQuery(GroupHelper.class).field("roomId").equal(roomId).field("helperId").equal(helperId);
		if(null!=query.get()){
			return JSONMessage.failure("?????????");
		}
		Query<Helper> helQuery = SKBeanUtils.getDatastore().createQuery(Helper.class).field("_id").equal(new ObjectId(helperId));
		if(null==helQuery.get()){
			return JSONMessage.failure("??????????????????");
		}
		GroupHelper entity = new GroupHelper();
		entity.setHelperId(helperId);
		entity.setRoomId(roomId);
		entity.setRoomJid(roomJid);
		entity.setUserId(userId);
		if(null==entity.getId())
			entity.setId(ObjectId.get());
		SKBeanUtils.getDatastore().save(entity);
		entity.setHelper(helQuery.get());
		return JSONMessage.success(null, entity);
	}
	
	/**
	 * ???????????????????????????
	 * @param roomId
	 * @param helperId
	 * @param keyWord
	 * @param value
	 * @return
	 */
	public JSONMessage addAutoResponse(String roomId,String helperId,String keyWord,String value){
		Query<GroupHelper> query = SKBeanUtils.getDatastore().createQuery(GroupHelper.class).field("roomId").equal(roomId).field("helperId").equal(helperId);
		UpdateOperations<GroupHelper> ops = SKBeanUtils.getDatastore().createUpdateOperations(GroupHelper.class);
		
		GroupHelper.KeyWord keyword= new GroupHelper.KeyWord();
		keyword.setId(ObjectId.get().toString());
		keyword.setKeyWord(keyWord);
		keyword.setValue(value);
		
		List<KeyWord> list = new ArrayList<>();
		
		GroupHelper groupHelper = query.get();
		if(null==groupHelper){
			return JSONMessage.failure("?????????????????????");
		}
		
		if(null!=groupHelper.getKeywords()){
			for(int i=0;i<groupHelper.getKeywords().size();i++){
				if(groupHelper.getKeywords().get(i).getKeyWord().equals(keyWord)){
					return JSONMessage.failure("??????????????????");
				}
			}
			groupHelper.getKeywords().add(keyword);
		}else{
			list.add(keyword);
			groupHelper.setKeywords(list);
		}
		
		ops.set("keywords", groupHelper.getKeywords());
		SKBeanUtils.getDatastore().update(query, ops);
		return JSONMessage.success(null, keyword);
		
	}
	
	/**
	 * ????????????????????????????????????
	 * @param id
	 * @param keyWordId
	 * @param keyword
	 * @param value
	 */
	public JSONMessage updateKeyword(String groupHelperId,String keyWordId,String keyword,String value){
		Query<GroupHelper> query = SKBeanUtils.getDatastore().createQuery(GroupHelper.class).field("_id").equal(new ObjectId(groupHelperId));
		UpdateOperations<GroupHelper> ops=SKBeanUtils.getDatastore().createUpdateOperations(GroupHelper.class);
		GroupHelper groupHelper =query.get();
		if(null==groupHelper){
			return JSONMessage.failure("?????????????????????");
		}
		if(null==groupHelper.getKeywords()){
			return JSONMessage.failure("??????????????????");
		}
		for(int i=0;i<groupHelper.getKeywords().size();i++){
			if(groupHelper.getKeywords().get(i).getId().equals(keyWordId)){
				groupHelper.getKeywords().get(i).setKeyWord(keyword);
				groupHelper.getKeywords().get(i).setValue(value);
			}
		}
		ops.set("keywords", groupHelper.getKeywords());
		SKBeanUtils.getDatastore().update(query, ops);
		return JSONMessage.success();
	}
	
	/**
	 * ???????????????????????????
	 * @param groupHelperId
	 * @param keyWordId
	 */
	public JSONMessage deleteAutoResponse(Integer userId,String groupHelperId,String keyWordId){
		Query<GroupHelper> query = SKBeanUtils.getDatastore().createQuery(GroupHelper.class).field("_id").equal(new ObjectId(groupHelperId)).field("userId").equal(userId);
		UpdateOperations<GroupHelper> ops=SKBeanUtils.getDatastore().createUpdateOperations(GroupHelper.class);
		GroupHelper groupHelper = query.get();
		if(null==groupHelper){
			return JSONMessage.failure("?????????????????????");
		}
		if(null==groupHelper.getKeywords()){
			return JSONMessage.failure("??????????????????");
		}
		for(int i=0;i<groupHelper.getKeywords().size();i++){
			if(groupHelper.getKeywords().get(i).getId().equals(keyWordId)){
				groupHelper.getKeywords().remove(i);
			}
		}
		ops.set("keywords", groupHelper.getKeywords());
		SKBeanUtils.getDatastore().update(query, ops);
		return JSONMessage.success();
	}
	
	/**
	 * ???????????????
	 * @param id
	 */
	public void deleteGroupHelper(Integer userId,String id){
		Query<GroupHelper> query = SKBeanUtils.getDatastore().createQuery(GroupHelper.class).field("_id").equal(new ObjectId(id)).field("userId").equal(userId);
		WriteResult delete = SKBeanUtils.getDatastore().delete(query);
		if(delete.getN()<=0){
			throw new ServiceException("????????????");
		}
	}
	
	/**
	 * ?????????????????????
	 * @param roomId
	 * @return
	 */
	public List<GroupHelper> queryGroupHelper(String roomId,String helperId){
		Query<GroupHelper> query = SKBeanUtils.getDatastore().createQuery(GroupHelper.class).field("roomId").equal(roomId);
		List<GroupHelper> list= query.asList();
		List<GroupHelper> newList = new ArrayList<>();
		if(!StringUtil.isEmpty(helperId)){
			for(int i=0;i<list.size();i++){
				if(list.get(i).getHelperId().equals(helperId)){
					Query<Helper> q = SKBeanUtils.getDatastore().createQuery(Helper.class).field("_id").equal(new ObjectId(list.get(i).getHelperId()));
					list.get(i).setHelper(q.get());
					newList.add(list.get(i));
				}
			}
		}else{
			for(int i=0;i<list.size();i++){
				Query<Helper> q = SKBeanUtils.getDatastore().createQuery(Helper.class).field("_id").equal(new ObjectId(list.get(i).getHelperId()));
				if(null!=q.get()){
					list.get(i).setHelper(q.get());
					newList.add(list.get(i));
				}
			}
		}
		
		return newList;
		
	}
	
	/** @Description:???????????????????????????????????????
	* @param userId
	* @param nickName
	* @param toUserId
	* @param toNickName
	* @param type
	* @param roomId
	**/ 
	public void multipointLoginUpdateUserInfo(Integer userId,String nickName,Integer toUserId,String toNickName,ObjectId roomId){
		Datastore datastore = SKBeanUtils.getDatastore();
		updateRoomInfo(userId, nickName,toUserId,toNickName,roomId);
		Query<OfflineOperation> query = datastore.createQuery(OfflineOperation.class).field("userId").equal(userId);
		query.field("friendId").equal(String.valueOf(roomId));
		if(null == query.get())
			datastore.save(new OfflineOperation(userId, KConstants.MultipointLogin.TAG_ROOM, String.valueOf(roomId), DateUtil.currentTimeSeconds()));
		else{
			UpdateOperations<OfflineOperation> ops = datastore.createUpdateOperations(OfflineOperation.class);
			ops.set("operationTime", DateUtil.currentTimeSeconds());
			datastore.update(query, ops);
		}
	}
	
	/** @Description:?????????????????????????????????????????????
	* @param userId
	* @param nickName
	**/ 
	public void updateRoomInfo(Integer userId,String nickName,Integer toUserId,String toNickName,ObjectId roomId){
		ThreadUtil.executeInThread(new Callback() {
			@Override
			public void execute(Object obj) {
					MessageBean messageBean=new MessageBean();
					messageBean.setType(KXMPPServiceImpl.updateRoomInfo);
					messageBean.setFromUserId(String.valueOf(userId));
					messageBean.setFromUserName(nickName);
					messageBean.setToUserId(String.valueOf(roomId));
					messageBean.setToUserName(getRoomName(roomId));
					messageBean.setMessageId(StringUtil.randomUUID());
					messageBean.setTo(String.valueOf(userId));
					try {
						KXMPPServiceImpl.getInstance().send(messageBean);
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
		});
	}
	
	public Room copyRoom(User user,String roomId){
		ObjectId objRoomId = new ObjectId(roomId);
		Room room = getRoom(objRoomId);
		List<Integer> memberIdList = getMemberIdList(objRoomId);
		memberIdList.remove(user.getUserId());
		room.setId(new ObjectId());
		String jid = SKBeanUtils.getXmppService().createMucRoom(user.getPassword(), user.getUserId().toString(),
				room.getName(),null, room.getSubject(), room.getDesc());
		room.setJid(jid);
		Room newRoom = add(user, room, memberIdList);
		return newRoom;
	}
}
