package com.application.base.elastic.util.jest;

import com.application.base.utils.common.BaseStringUtil;
import com.application.base.utils.common.PropStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * ES(5.5.0) 客户端建立.
 * @author 孤狼
 */
public class EsJestClientBuilder {

	static Logger logger = LoggerFactory.getLogger(EsJestClientBuilder.class.getName());
	
	/**
	 * 客户端：配置文件获得.
	 */
	private RestHighLevelClient settingClient;
	
	/**
	 * 客户端：参数获得.
	 */
	private RestHighLevelClient  paramClient;
	
	/**
	 * node IP 地址.
	 */
	private String serverIPs="127.0.0.1:9200";
	/**
	 * node auth.
	 */
	private String serverAuth = "elastic:elastic";
	
	/**
	 *配置文件信息记录
	 */
	private String infoPath="/es.properties";
	
	/**
	 * 读取文件初始化.
	 * ES RestHighLevelClient 客户端连接
	 * 在elasticsearch平台中,
	 * 可以执行创建索引,获取索引,删除索引,搜索索引等操作
	 * @return TransportClient
	 */
	@SuppressWarnings("unchecked")
	public RestHighLevelClient initSettingsClient(String inputPath) {
		try {
			if (settingClient == null) {
				synchronized (EsJestClientBuilder.class){
					if (settingClient == null) {
						if (!BaseStringUtil.isEmpty(inputPath)){
							infoPath=inputPath;
						}
						Map<String,String> values = PropStringUtils.getValues(infoPath);
						if (values.isEmpty()){
							logger.info("根据配置文件:"+infoPath+"获取的配置信息为空!");
							return null;
						}
						//初始化操作实现
						serverIPs = values.get("elastic.serverIps");
						serverAuth = values.get("elastic.auth");
						settingClient=initClient(serverIPs,serverAuth);
					}
				}
				return settingClient;
			} else {
				return settingClient;
			}
		} catch (Exception e) {
			logger.error("创建 RestHighLevelClient 对象出现异常,异常信息是{}",e.getMessage());
			try {
				if (settingClient != null) {
					settingClient.close();
				}
			}catch (Exception e1){
				logger.error("关闭 RestHighLevelClient 对象出现异常,异常信息是{}",e.getMessage());
			}
			return null;
		}
	}
	
	/**
	 * 返回创建的客户端信息
	 * @param serverIPs
	 * @param serverAuth
	 * @return
	 */
	private RestHighLevelClient initClient(String serverIPs,String serverAuth) {
		Map<String, Integer> data = parseNodeIps(serverIPs);
		HttpHost[] hosts = new HttpHost[data.size()];
		int index=0;
		for (Map.Entry<String,Integer> entry : data.entrySet()) {
			hosts[index] = new HttpHost(entry.getKey(),entry.getValue(),"http");
			index++;
		}
		RestClientBuilder builder = RestClient.builder(hosts);
		//安全认证.
		if (StringUtils.isNotBlank(serverAuth)){
			String name = serverAuth.split(":")[0];
			String pass = serverAuth.split(":")[1];
			final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			//es账号密码（默认用户名为elastic）
			credentialsProvider.setCredentials(AuthScope.ANY,new UsernamePasswordCredentials(name, pass));
			//添加认证信息.
			builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
				@Override
				public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
					httpClientBuilder.disableAuthCaching();
					return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
				}
			});
		}
		RestHighLevelClient levelClient = new RestHighLevelClient(builder);
		return levelClient;
	}
	
	
	/**
	 * 通过参数获得对象.
	 * ES TransPortClient 客户端连接
	 * 在elasticsearch平台中,
	 * 可以执行创建索引,获取索引,删除索引,搜索索引等操作
	 * @param serverIPs：集群的ip端口,单个：192.168.1.1:9300;多个 ：192.168.1.1:9300;192.168.1.2:9300
	 * @return TransportClient
	 */
	@SuppressWarnings("unchecked")
	public RestHighLevelClient initParamsClient(String serverIPs,String serverAuth) {
		try {
			if (paramClient == null) {
				synchronized (EsJestClientBuilder.class){
					//初始化操作实现
					paramClient=initClient(serverIPs,serverAuth);
				}
				return paramClient;
			} else {
				return paramClient;
			}
		} catch (Exception e) {
			logger.error("创建 TransportClient 对象出现异常,异常信息是{}",e.getMessage());
			try {
				if (settingClient != null) {
					settingClient.close();
				}
			}catch (Exception e1){
				logger.error("关闭 RestHighLevelClient 对象出现异常,异常信息是{}",e.getMessage());
			}
			return null;
		}
	}
	
	/**
	 * 解析节点IP信息,多个节点用逗号隔开,IP和端口用冒号隔开
	 * @return
	 */
	private Map<String, Integer> parseNodeIps(String serverIPs) {
		String[] nodeIpInfoArr = serverIPs.split(",");
		Map<String, Integer> resultMap = new HashMap<String, Integer>(nodeIpInfoArr.length);
		for (String ipInfo : nodeIpInfoArr) {
			String[] ipInfoArr = ipInfo.split(":");
			resultMap.put(ipInfoArr[0], Integer.parseInt(ipInfoArr[1]));
		}
		return resultMap;
	}
	
	public String getServerIPs() {
		return serverIPs;
	}
	public void setServerIPs(String serverIPs) {
		this.serverIPs = serverIPs;
	}
	
	public String getServerAuth() {
		return serverAuth;
	}
	
	public void setServerAuth(String serverAuth) {
		this.serverAuth = serverAuth;
	}
}
