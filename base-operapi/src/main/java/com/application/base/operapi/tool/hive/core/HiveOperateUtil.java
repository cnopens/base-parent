package com.application.base.operapi.tool.hive.core;

import com.application.base.operapi.core.ColumnInfo;
import com.application.base.operapi.core.hdfs.HadoopConfig;
import com.application.base.operapi.tool.hive.common.config.OperateConfig;
import com.application.base.operapi.tool.hive.common.constant.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : 孤狼
 * @NAME: HiveOperateUtil
 * @DESC: hive的操作类
 **/
public class HiveOperateUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(HiveOperateUtil.class);
	private static  HiveJdbcUtil hiveJdbcClient = null;
	private static  Ssh2ServerUtil remoteOpera=null;
	private static  HiveOperateUtil instance = null;
	private static HdfsOperUtil hdfsOperUtil=null;
	
	/**
	 * operate配置.
	 */
	private OperateConfig operateConfig;
	
	public OperateConfig getOperateConfig() {
		return operateConfig;
	}
	public void setOperateConfig(OperateConfig operateConfig) {
		this.operateConfig = operateConfig;
	}
	
	/**
	 * 单例模式.
	 * @param operateConfig
	 * @return
	 */
	public static synchronized HiveOperateUtil getInstance(OperateConfig operateConfig) {
		if (instance == null) {
			hiveJdbcClient = HiveJdbcUtil.getInstance(operateConfig.getHiveConfig());
			remoteOpera = Ssh2ServerUtil.getInstance(operateConfig.getSshConfig());
			hdfsOperUtil = HdfsOperUtil.getInstance(operateConfig.getHadoopConfig());
			instance = new HiveOperateUtil(operateConfig);
		}
		return instance;
	}
	
	/**
	 * 构造函数.
	 * @param operateConfig
	 */
	public HiveOperateUtil(OperateConfig operateConfig) {
		this.operateConfig = operateConfig;
	}
	
	/**
	 * 获取表信息.
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public List<ColumnInfo> getTableColumnInfo(String tableName) throws Exception {
        List<ColumnInfo> resultList = new ArrayList<ColumnInfo>();
        List<Map<String,String>> resultMapList = hiveJdbcClient.describeTable(tableName);
        for(int i=0;i<resultMapList.size();i++){
            if (resultMapList.get(i).get(Constant.ROW_NAME).equals(Constant.ROW_ID)){
            	continue;
            }
            resultList.add(new ColumnInfo(resultMapList.get(i).get(Constant.ROW_NAME),resultMapList.get(i).get(Constant.DATA_TYPE)));
        }
        return resultList;
    }

    /**
     * 获取生成hive表的hiveql
     * @param tableName
     * @param columnInfoList
     * @return
     */
    public String genCreateTablesqlByColumnInfo(String tableName,List<ColumnInfo> columnInfoList,String split){
        String sql = "create table "+tableName+"(";
        for (ColumnInfo columnInfo:columnInfoList){
            sql +=  columnInfo.getColumnName() +"  " +columnInfo.getColumnDbType() + ",";
        }
        sql = sql.substring(0,sql.length()-1);
        sql += ") row format delimited fields terminated by ',' stored as textfile";
        return sql;
    }
    
    
	/**
	 *文件上传并且导入到hive中
	 * @param tableName
	 * @param tmpFile
	 * @param columnMapList
	 * @param split
	 * @param localFilePath
	 * @return
	 * @throws Exception
	 */
    public String  executeHiveOperate(String tableName,String tmpFile,List<ColumnInfo> columnMapList,String split,String localFilePath)throws Exception{
        String result = tableName ;
	    HadoopConfig hadoopConfig = operateConfig.getHadoopConfig();
	    String absolutePath = hadoopConfig.getHdfsFilePath();
        //上传服务器
	    HdfsOperUtil operUtil = HdfsOperUtil.getInstance(hadoopConfig);
	
	    //不能使用ssh的方式传递文件.得用hadoop的方式来搬运文件.
	    boolean bool = operUtil.uploadFileToHdfs(localFilePath+tmpFile,absolutePath);
	    //boolean bool = remoteOpera.uploadFile(localFilePath+tmpFile,absolutePath);
	    if (!bool){
	    	logger.error("上传文件"+localFilePath+tmpFile+"失败了.");
	    	return "";
	    }
        //根据表名 表头 拼接sql
        String createTableSql = genCreateTablesqlByColumnInfo(tableName,columnMapList,split);
        logger.info(createTableSql);
        try {
            //建 hive 表
            bool = hiveJdbcClient.excuteHiveql(createTableSql);
	
            //在linx系统上,执行如下命令没有问题.
	        //String loadStr = "load data local inpath '" + absolutePath + tmpFile + "' overwrite into table "+tableName+" ";
	        //远程需要用hdfs来操作.
            String loadStr = "load data inpath 'hdfs:" + absolutePath + tmpFile + "' overwrite into table "+tableName+" ";
	        //String loadStr = "load data inpath 'hdfs://192.168.10.185:8020" + absolutePath + tmpFile + "' into table "+tableName+" ";
	        //在linx系统上,执行如下命令没有问题.
	        //remoteOpera.excuteCmd("chmod -R 755 "+absolutePath + tmpFile);
	        
            System.out.println("loadStr \n\t"+loadStr);
            bool = hiveJdbcClient.excuteHiveql(loadStr);
			
            //shell 删除临时文件
            if (hadoopConfig.isDeleteFile()){
	            //在 hadoop 环境下执行是 ok 的.
	            // String execCommand = "hadoop fs -rm -f " + absolutePath + "/" + tmpFile;
	            String execCommand = absolutePath + tmpFile;
	            System.out.println("execCommand \n\t"+execCommand);
				hdfsOperUtil.delFile(execCommand);
	            //在linx系统上,执行如下命令没有问题.
	            //remoteOpera.excuteCmd(execCommand);
            }
        } catch (Exception e) {
        	e.printStackTrace();
            logger.error("error: ",e);
            throw  new RuntimeException(e.getCause().getLocalizedMessage());
        }
        return  result ;
    }
}
