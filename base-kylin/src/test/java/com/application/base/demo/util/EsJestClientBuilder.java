package com.application.base.demo.util;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * ES( 6.x ) 客户端建立.
 * @author 孤狼
 */
public class EsJestClientBuilder {

	static Logger logger = LoggerFactory.getLogger(EsJestClientBuilder.class.getName());
	
	/**
	 * 客户端：参数获得.
	 */
	private RestHighLevelClient paramClient;
	
	/**
	 * node IP 地址.
	 */
	private String serverIPs="127.0.0.1:9200";
	
	
	/**
	 * 返回创建的客户端信息
	 * @param serverIPs
	 * @return
	 */
	private RestHighLevelClient initClient(String serverIPs) {
		Map<String, Integer> data = parseNodeIps(serverIPs);
		HttpHost[] hosts = new HttpHost[data.size()];
		int index=0;
		for (Map.Entry<String,Integer> entry : data.entrySet()) {
			hosts[index] = new HttpHost(entry.getKey(),entry.getValue(),"http");
			index++;
		}
		RestClientBuilder builder = RestClient.builder(hosts);
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
	public RestHighLevelClient initParamsClient(String serverIPs) {
		try {
			if (paramClient == null) {
				synchronized (EsJestClientBuilder.class){
					//初始化操作实现
					paramClient=initClient(serverIPs);
				}
				return paramClient;
			} else {
				return paramClient;
			}
		} catch (Exception e) {
			logger.error("创建 TransportClient 对象出现异常,异常信息是{}",e.getMessage());
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
	
}
