package com.shiku.mianshi.controller;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayFundTransOrderQueryRequest;
import com.alipay.api.request.AlipayFundTransToaccountTransferRequest;
import com.alipay.api.response.AlipayFundTransOrderQueryResponse;
import com.alipay.api.response.AlipayFundTransToaccountTransferResponse;
import com.alipay.util.AliPayParam;
import com.alipay.util.AliPayUtil;

import cn.xyz.commons.constants.KConstants;
import cn.xyz.commons.constants.KConstants.ResultCode;
import cn.xyz.commons.utils.BeanUtils;
import cn.xyz.commons.utils.DateUtil;
import cn.xyz.commons.utils.NumberUtil;
import cn.xyz.commons.utils.ReqUtil;
import cn.xyz.commons.utils.StringUtil;
import cn.xyz.commons.vo.JSONMessage;
import cn.xyz.mianshi.service.impl.TransfersRecordManagerImpl;
import cn.xyz.mianshi.utils.SKBeanUtils;
import cn.xyz.mianshi.vo.AliPayTransfersRecord;
import cn.xyz.mianshi.vo.ConsumeRecord;
import cn.xyz.mianshi.vo.User;
import cn.xyz.service.AuthServiceUtils;

@RestController
@RequestMapping("/alipay")
public class AlipayController extends AbstractController{
	
	@Autowired
	private TransfersRecordManagerImpl transfersManager;
	
	@RequestMapping("/callBack")
	public JSONMessage payCheck(HttpServletRequest request, HttpServletResponse response){
		Map<String,String> params = new HashMap<String,String>();
		Map requestParams = request.getParameterMap();
		for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
		    String name = (String) iter.next();
		    String[] values = (String[]) requestParams.get(name);
		    String valueStr = "";
		    for (int i = 0; i < values.length; i++) {
		        valueStr = (i == values.length - 1) ? valueStr + values[i]
		                    : valueStr + values[i] + ",";
		  	}
		    //??????????????????????????????????????????????????????
			//valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
			params.put(name, valueStr);
		}
		try {
			String tradeNo = params.get("out_trade_no");
			String tradeStatus=params.get("trade_status");
					
			logger.info("?????????    "+tradeNo);
			
			boolean flag = AlipaySignature.rsaCheckV1(params,AliPayUtil.ALIPAY_PUBLIC_KEY, AliPayUtil.CHARSET,"RSA2");
			if(flag){
				ConsumeRecord entity = SKBeanUtils.getConsumeRecordManager().getConsumeRecordByNo(tradeNo);
				if(null==entity)
					logger.info("?????????  ?????? ????????? {} ",tradeNo);
				if(entity.getStatus()!=KConstants.OrderStatus.END&&"TRADE_SUCCESS".equals(tradeStatus)){
					//????????????????????????????????????????????????
					AliPayParam aliCallBack=new AliPayParam();
					BeanUtils.populate(aliCallBack, params);
					SKBeanUtils.getConsumeRecordManager().saveEntity(aliCallBack);
					User user=SKBeanUtils.getUserManager().get(entity.getUserId());
					user.setAliUserId(aliCallBack.getBuyer_id());
					SKBeanUtils.getUserManager().rechargeUserMoeny(entity.getUserId(), entity.getMoney(), KConstants.MOENY_ADD);
					entity.setStatus(KConstants.OrderStatus.END);
					entity.setOperationAmount(entity.getMoney());
					entity.setCurrentBalance(user.getBalance());
					SKBeanUtils.getConsumeRecordManager().update(entity.getId(), entity);
					logger.info("????????????????????? {}",tradeNo);
				}else if("TRADE_CLOSED".equals(tradeStatus)) {
					logger.info("?????????  ?????????  {}  ",tradeNo);
					SKBeanUtils.getConsumeRecordManager().updateAttribute(entity.getId(), "status", -1);
					return JSONMessage.success();
				}
				return JSONMessage.success();
			}else{
				logger.info("?????????????????????"+flag);
				return JSONMessage.failure(null);
				
			}
		} catch (AlipayApiException e) {
			e.printStackTrace();
			return JSONMessage.failure(e.getMessage());
		}
	}
	
	/**
	 * ???????????????
	 * @param amount
	 * @param time
	 * @param secret
	 * @param callback
	 * @return
	 */
	@RequestMapping(value = "/transfer")
	public JSONMessage transfer(@RequestParam(defaultValue="") String amount,@RequestParam(defaultValue="0") long time,
			@RequestParam(defaultValue="") String secret, String callback){
		if(StringUtil.isEmpty(amount)) {
			return JSONMessage.failure("????????????????????????");
		}else if(StringUtil.isEmpty(secret)) {
			return JSONMessage.failure("??????????????????");
		}
		
		
		int userId = ReqUtil.getUserId();
		User user=SKBeanUtils.getUserManager().get(userId);
		String token = getAccess_token();
		if(StringUtil.isEmpty(user.getAliUserId())){
			return JSONMessage.failure("?????? ??????????????? ???????????????????????? ");
		}else if(!AuthServiceUtils.authWxTransferPay(user.getPayPassword(),userId+"", token, amount,user.getAliUserId(),time, secret)){
			return JSONMessage.failure("??????????????????");
		}
		return aliWithdrawalPay(user, amount);
		
	}
	/**
	 * ???????????????
	 * @param amount
	 * @param time
	 * @param secret
	 * @param callback
	 * @return
	 */
	@RequestMapping(value = "/transfer/v1")
	public JSONMessage transferV1(@RequestParam(defaultValue="") String data,
	@RequestParam(defaultValue="") String codeId, String callback){
		int userId = ReqUtil.getUserId();
		User user=SKBeanUtils.getUserManager().get(userId);
		String token = getAccess_token();
		if(StringUtil.isEmpty(user.getAliUserId())){
			return JSONMessage.failure("?????? ??????????????? ???????????????????????? ");
		}
		String code = SKBeanUtils.getRedisService().queryTransactionSignCode(userId, codeId);
		if(StringUtil.isEmpty(code))
			return JSONMessage.failureByErrCode(ResultCode.AUTH_FAILED);
		JSONObject jsonObj = AuthServiceUtils.authWxWithdrawalPay(userId+"", token, data, code, user.getPayPassword());
		if(null==jsonObj) {
			return JSONMessage.failureByErrCode(ResultCode.AUTH_FAILED);
		}
		String amount = jsonObj.getString("amount");
		if(StringUtil.isEmpty(amount)) {
			return JSONMessage.failure("????????????????????????");
		}
		
		return aliWithdrawalPay(user, amount);
		
	}
	
	public JSONMessage aliWithdrawalPay(User user, String amount) {
		int userId=user.getUserId();
		// ????????????
		double total=(Double.valueOf(amount));
		if(100<total) {
			return JSONMessage.failure("????????????  ?????? 100???");
		}
		
		/**
		 * ??????????????? 0.6%
		 * ?????????????????????????????????????????????????????????0.6%??????????????????????????????0.6%?????????
		 */
		DecimalFormat df = new DecimalFormat("#.00");
		double fee =Double.valueOf(df.format(total*0.006));
		if(0.01>fee) {
			fee=0.01;
		}else  {
			fee=NumberUtil.getCeil(fee, 2);
		}
		
		/**
		 * 
		 * ??????????????????  = ????????????-?????????
		 */
		Double totalFee= Double.valueOf(df.format(total-fee));
		
		if(totalFee>user.getBalance()) {
			return JSONMessage.failure("?????????????????? ???????????? ");
		}
		String orderId=StringUtil.getOutTradeNo();
		AliPayTransfersRecord record=new AliPayTransfersRecord();
		record.setUserId(userId);
		record.setAppid(AliPayUtil.APP_ID);
		record.setOutTradeNo(orderId);
		record.setAliUserId(user.getAliUserId());
		record.setTotalFee(amount);
		record.setFee(fee+"");
		record.setRealFee(totalFee+"");
		record.setCreateTime(DateUtil.currentTimeSeconds());
		record.setStatus(0);
		
		AlipayFundTransToaccountTransferRequest request = new AlipayFundTransToaccountTransferRequest();
//		request.setBizModel(bizModel);
		
		request.setBizContent("{" +
				"    \"out_biz_no\":\""+orderId+"\"," +  // ??????Id
				"    \"payee_type\":\"ALIPAY_USERID\"," + // ????????????????????????
				"    \"payee_account\":\""+user.getAliUserId()+"\"," + // ?????????
				"    \"amount\":\""+totalFee+"\"," +	// ??????
				"    \"payer_show_name\":\"????????????\"," +
				"    \"remark\":\"????????????\"," +
				"  }");
		try {
			AlipayFundTransToaccountTransferResponse response = AliPayUtil.getAliPayClient().execute(request);
			System.out.println("??????????????????  "+response.getCode());
			if(response.isSuccess()){
				record.setResultCode(response.getCode());
				record.setCreateTime(DateUtil.toTimestamp(response.getPayDate()));
				record.setStatus(1);
				transfersManager.transfersToAliPay(record);
				
				logger.info("?????????????????????");
				return JSONMessage.success();
			} else {
				record.setErrCode(response.getErrorCode());
				record.setErrDes(response.getMsg());
				record.setStatus(-1);
				transfersManager.saveEntity(record);
				logger.info("?????????????????????");
				return JSONMessage.failure("?????????????????????");
			}
		} catch (AlipayApiException e) {
			e.printStackTrace();
			return JSONMessage.failure("?????????????????????");
		}
	}
	
	/**
	 * ?????????????????????
	 * @param tradeno
	 * @param callback
	 * @return
	 */
	@RequestMapping(value ="/aliPayQuery")
	public JSONMessage aliPayQuery(String tradeno,String callback){
		if (StringUtil.isEmpty(tradeno)) {
			return null;
		}
		AlipayFundTransOrderQueryRequest request = new AlipayFundTransOrderQueryRequest();
		request.setBizContent("{" +
				"\"out_biz_no\":\""+tradeno+"\"," + // ?????????
				"\"order_id\":\"\"" +
				"  }");
		try {
			AlipayFundTransOrderQueryResponse response = AliPayUtil.getAliPayClient().execute(request);
			logger.info("??????????????????  "+response.getCode());
			if(response.isSuccess()){
				logger.info("????????????");
			} else {
				logger.info("????????????");
			}
		} catch (AlipayApiException e) {
			e.printStackTrace();
		}

		return JSONMessage.success();
	}
}
