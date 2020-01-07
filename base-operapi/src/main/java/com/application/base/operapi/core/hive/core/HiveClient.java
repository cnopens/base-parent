package com.application.base.operapi.core.hive.core;

import com.alibaba.fastjson.JSON;
import com.application.base.operapi.core.ColumnInfo;
import com.application.base.operapi.core.hive.rdbs.HiveDataType;
import com.application.base.operapi.core.hive.rdbs.JavaDataType;
import com.application.base.operapi.core.hive.rdbs.MysqlDataType;
import com.application.base.operapi.core.hive.rdbs.OracleDataType;
import com.application.base.operapi.core.hive.rdbs.RdbmsType;
import com.application.base.operapi.core.hive.rdbs.SqlServerDataType;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author : 孤狼
 * @NAME: HiveClient
 * @DESC: 操作 hive 的client
 **/
public class HiveClient {
	
	
	/**
	 * 测试是否可以使用
	 * @param args
	 */
	public static void main(String[] args) {
		
		//这里是Oracle连接方法
		/*
		String url = "jdbc:oracle:thin:@192.168.12.44:1521:orcl";
		String user = "bdc";
		String pwd = "bdc123";
		String table = "FZ_USER_T";
		*/
		
		//这里是SQL Server连接方法
		/*
		String url = "jdbc:sqlserver://localhost:1433;DateBaseName=数据库名";
		String uid = "sa";
		String pwd = "sa";
		String table = "FZ_USER_T";
		*/
		
		//mysql
		String user = "root";
		String pwd = "123456";
		String url = "jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterEncoding=UTF-8";
		String table = "sum_data_dir";
		
		List list = getTableMapInfo(url,user,pwd,table, RdbmsType.MYSQL);
		System.out.println(list);
		
		List<ColumnInfo> dataList = getTableBeanInfo(url,user,pwd,table, RdbmsType.MYSQL);
		System.out.println(JSON.toJSONString(dataList));
		
		System.out.println("create hive sql : \n\t"+getCreateHiveTableSql(url,user,pwd,table,RdbmsType.MYSQL));
		
	}
	
	/**
	 * 获得创建 hive 的 sql.
	 * @param conn
	 * @param tableName
	 * @param rdbmsType
	 * @return
	 */
	public static String getCreateHiveTableSql(Connection conn, String tableName,RdbmsType rdbmsType){
		List<ColumnInfo> columnInfos = getTableBeanInfo(conn,tableName,rdbmsType);
		StringBuffer buffer = new StringBuffer( "create table if not exists `"+tableName+"` (");
		for (int i = 0; i <columnInfos.size() ; i++) {
			ColumnInfo info = columnInfos.get(i);
			if (i!=columnInfos.size()-1){
				buffer.append(" `"+info.getColumnName()+"` "+info.getColumnHiveType()+"  COMMENT '"+info.getColumnComment()+"',");
			}else{
				buffer.append(" `"+info.getColumnName()+"` "+info.getColumnHiveType()+"  COMMENT '"+info.getColumnComment()+"'");
			}
		}
		buffer.append(") row format delimited fields terminated by '\t' null defined as '' stored as textfile ");
		return buffer.toString();
	}
	
	/**
	 * 获得创建 hive 的 sql.
	 * @param url
	 * @param user
	 * @param pwd
	 * @param tableName
	 * @param rdbmsType
	 * @return
	 */
	public static String getCreateHiveTableSql(String url, String user, String pwd, String tableName,RdbmsType rdbmsType){
		Connection conn = getConnections(rdbmsType.getDriver(),url,user,pwd);
		return getCreateHiveTableSql(conn,tableName,rdbmsType);
	}
	
	/**
	 * 获取数据库表的信息.
	 * @param conn
	 * @param table
	 * @param rdbmsType 数据库类型:mysql,oracle,sql server
	 * @return
	 */
	public static List<Map<String,Object>> getTableMapInfo(Connection conn, String table,RdbmsType rdbmsType){
		List<Map<String,Object>> result = new ArrayList();
		DatabaseMetaData dbmd = null;
		ResultSet resultSet = null;
		ResultSet rs = null;
		try {
			dbmd = conn.getMetaData();
			resultSet = dbmd.getTables(null, "%", table, new String[] { "TABLE" });
			while (resultSet.next()) {
				String tableName=resultSet.getString("TABLE_NAME");
				System.out.println(tableName);
				if(tableName.equals(table)){
					rs = conn.getMetaData().getColumns(null, getSchema(conn),tableName.toUpperCase(), "%");
					while(rs.next()){
						Map<String,Object> map = new HashMap();
						String colName = rs.getString("COLUMN_NAME");
						map.put("columnName", colName);
						String remarks = rs.getString("REMARKS");
						if(remarks == null || remarks.equals("")){
							remarks = colName;
						}
						map.put("columnComment",remarks);
						String dbType = rs.getString("TYPE_NAME");
						map.put("columnDbType",dbType);
						
						map.put("columnJavaType",changeJavaType(dbType,rdbmsType));
						map.put("columnHiveType",changeHiveType(dbType,rdbmsType));
						map.put("columnValueType",changeValueType(dbType));
						result.add(map);
					}
				}
			}
			rs.close();
			resultSet.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	
	
	/**
	 * 根据数据库的连接参数，获取指定表的基本信息：字段名、字段类型、字段注释
	 * @param url 数据库连接url
	 * @param user	数据库登陆用户名
	 * @param pwd 数据库登陆密码
	 * @param tableName	表名
	 * @return Map集合
	 */
	public static List<Map<String,Object>> getTableMapInfo(String url, String user, String pwd, String tableName,RdbmsType rdbmsType){
		Connection conn = null;
		List<Map<String,Object>> dataList = null;
		try {
			conn = getConnections(rdbmsType.getDriver(),url,user,pwd);
			dataList = getTableMapInfo(conn,tableName,rdbmsType);
		}catch (Exception e){
			e.printStackTrace();
		}finally {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return dataList;
	}
	
	/**
	 * 根据数据库的连接参数，获取指定表的基本信息：字段名、字段类型、字段注释
	 * @param conn 连接
	 * @param table	表名
	 * @return Map集合
	 */
	public static List<ColumnInfo> getTableBeanInfo(Connection conn, String table,RdbmsType rdbmsType){
		List<ColumnInfo> beanList = new ArrayList();
		DatabaseMetaData dbmd = null;
		ResultSet resultSet = null;
		ResultSet rs = null;
		try {
			dbmd = conn.getMetaData();
			resultSet = dbmd.getTables(null, "%", table, new String[] { "TABLE" });
			while (resultSet.next()) {
				String tableName=resultSet.getString("TABLE_NAME");
				System.out.println(tableName);
				if(tableName.equals(table)){
					rs = conn.getMetaData().getColumns(null, getSchema(conn),tableName.toUpperCase(), "%");
					while(rs.next()){
						ColumnInfo columnInfo = new ColumnInfo();
						Map<String,Object> map = new HashMap();
						String colName = rs.getString("COLUMN_NAME");
						columnInfo.setColumnName(colName);
						String remarks = rs.getString("REMARKS");
						if(remarks == null || remarks.equals("")){
							remarks = colName;
						}
						columnInfo.setColumnComment(remarks);
						String dbType = rs.getString("TYPE_NAME");
						columnInfo.setColumnDbType(dbType);
						String javaType = changeJavaType(dbType,rdbmsType);
						columnInfo.setColumnJavaType(javaType);
						String hiveType = changeHiveType(dbType,rdbmsType);
						columnInfo.setColumnHiveType(hiveType);
						String valueType = changeValueType(dbType);
						columnInfo.setColumnValueType(valueType);
						beanList.add(columnInfo);
					}
				}
			}
			rs.close();
			resultSet.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return beanList;
	}
	
	/**
	 * 根据数据库的连接参数，获取指定表的基本信息：字段名、字段类型、字段注释
	 * @param url 数据库连接url
	 * @param user	数据库登陆用户名
	 * @param pwd 数据库登陆密码
	 * @param table	表名
	 * @return Map集合
	 */
	public static List<ColumnInfo> getTableBeanInfo(String url, String user, String pwd, String table,RdbmsType rdbmsType){
		List<ColumnInfo> dataList = new ArrayList();
		Connection conn = null;
		try {
			conn = getConnections(rdbmsType.getDriver(),url,user,pwd);
			dataList = getTableBeanInfo(conn,table,rdbmsType);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return dataList;
	}
	
	/**
	 * 获取全表表数据
	 * @param tableName
	 * @param con
	 * @return
	 */
	public static List<List<Object>> getTableDatas(String tableName,Connection con){
		List<List<Object>> resultList = new ArrayList<>();
		ResultSet res = null ;
		Statement stmt = null ;
		try {
			stmt = con.createStatement();
			String sql = "select * from "+tableName;
			res = stmt.executeQuery(sql);
			ResultSetMetaData metaData = res.getMetaData();
			while (res.next()){
				List<Object> row = new ArrayList<>();
				for (int i=1;i<=metaData.getColumnCount();i++){
					row.add(getResultSetValue(res,i));
				}
				resultList.add(row);
			}
		}catch (Exception e){
			e.printStackTrace();
		}finally {
			close(res,stmt,con);
		}
		return resultList ;
	}
	/**
	 * 获得数据类型的值.
	 * @param rs
	 * @param index
	 * @return
	 */
	public static Object getResultSetValue(ResultSet rs, int index) {
		Object obj = null;
		try {
			obj = rs.getObject(index);
			String className = null;
			if (obj != null) {
				className = obj.getClass().getName();
			}
			if (obj instanceof Blob) {
				Blob blob = (Blob)obj;
				obj = blob.getBytes(1L, (int)blob.length());
			} else if (obj instanceof Clob) {
				Clob clob = (Clob)obj;
				obj = clob.getSubString(1L, (int)clob.length());
			} else if (!"oracle.sql.TIMESTAMP".equals(className) && !"oracle.sql.TIMESTAMPTZ".equals(className)) {
				if (className != null && className.startsWith("oracle.sql.DATE")) {
					String metaDataClassName = rs.getMetaData().getColumnClassName(index);
					if (!"java.sql.Timestamp".equals(metaDataClassName) && !"oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
						obj = rs.getDate(index);
					} else {
						obj = rs.getTimestamp(index);
					}
				} else if (obj instanceof Date && "java.sql.Timestamp".equals(rs.getMetaData().getColumnClassName(index))) {
					obj = rs.getTimestamp(index);
				}
			} else {
				obj = rs.getTimestamp(index);
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return obj;
	}
	
	/**
	 * 获取数据库表字段类型
	 * @param tableName
	 * @param con
	 * @return
	 */
	public static List<ColumnInfo> getTableColumnInfo(String tableName, Connection con){
		List<ColumnInfo> resultList = new ArrayList<>();
		try {
			Statement stmt = con.createStatement();
			String sql = "select * from "+tableName;
			ResultSet res = stmt.executeQuery(sql);
			ResultSetMetaData rsmd = res.getMetaData();
			for(int i=1;i<=rsmd.getColumnCount();i++) {
				resultList.add(new ColumnInfo(rsmd.getColumnName(i), rsmd.getColumnTypeName(i)));
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return resultList ;
	}
	
	/**
	 * 类型转换.
	 * @param dbType
	 * @return
	 */
	private static String changeValueType(String dbType) {
		dbType = dbType.toUpperCase();
		switch(dbType){
			case "VARCHAR":
			case "VARCHAR2":
			case "CHAR":
				return "1";
			case "FLOAT":
			case "DOUBLE":
			case "NUMBER":
			case "DECIMAL":
				return "4";
			case "INT":
			case "SMALLINT":
			case "INTEGER":
				return "2";
			case "BIGINT":
			case "LONG":
				return "6";
			case "DATETIME":
			case "TIMESTAMP":
			case "DATE":
				return "7";
			default:
				return "1";
		}
	}
	
	/**
	 * hive 类型转换.
	 * @param dataList
	 * @param rdbmsType
	 * @return
	 */
	public static List<ColumnInfo> convertTableColumnInfos(List<ColumnInfo> dataList,RdbmsType rdbmsType){
		for (ColumnInfo info : dataList) {
			convertTableColumnInfo(info,rdbmsType);
		}
		return dataList ;
	}
	
	/**
	 * 获得类型
	 * @param dbType
	 * @param rdbmsType
	 * @return
	 */
	private static String changeHiveType(String dbType, RdbmsType rdbmsType) {
		//hive 类型.
		String hiveType = HiveDataType.STRING;
		switch (rdbmsType){
			case MYSQL:
				hiveType = MysqlDataType.dataTypeConvertToHiveType(dbType);
				break;
			case SQLSERVER:
				hiveType = SqlServerDataType.dataTypeConvertToHiveType(dbType);
				break;
			case ORACLE:
				hiveType = OracleDataType.dataTypeConvertToHiveType(dbType);
				break;
		}
		return hiveType;
	}
	
	/**
	 * 获得类型.
	 * @param dbType
	 * @param rdbmsType
	 * @return
	 */
	private static String changeJavaType(String dbType,RdbmsType rdbmsType) {
		//java类型.
		String javaType = JavaDataType.STRING;
		switch (rdbmsType){
			case MYSQL:
				javaType = MysqlDataType.dataTypeConvertToJavaType(dbType);
				break;
			case SQLSERVER:
				javaType = SqlServerDataType.dataTypeConvertToJavaType(dbType);
				break;
			case ORACLE:
				javaType = OracleDataType.dataTypeConvertToJavaType(dbType);
				break;
		}
		return javaType ;
	}
	
	/**
	 * 类型转换.
	 * @param info
	 * @param rdbmsType
	 * @return
	 */
	public static ColumnInfo convertTableColumnInfo(ColumnInfo info,RdbmsType rdbmsType){
		//hive 类型.
		String hiveType = HiveDataType.STRING;
		switch (rdbmsType){
			case MYSQL:
				hiveType = MysqlDataType.dataTypeConvertToHiveType(info.getColumnDbType());
				break;
			case SQLSERVER:
				hiveType = SqlServerDataType.dataTypeConvertToHiveType(info.getColumnDbType());
				break;
			case ORACLE:
				hiveType = OracleDataType.dataTypeConvertToHiveType(info.getColumnDbType());
				break;
		}
		info.setColumnHiveType(hiveType);
		//java类型.
		String javaType = JavaDataType.STRING;
		switch (rdbmsType){
			case MYSQL:
				javaType = MysqlDataType.dataTypeConvertToJavaType(info.getColumnDbType());
				break;
			case SQLSERVER:
				javaType = SqlServerDataType.dataTypeConvertToJavaType(info.getColumnDbType());
				break;
			case ORACLE:
				javaType = OracleDataType.dataTypeConvertToJavaType(info.getColumnDbType());
				break;
		}
		info.setColumnJavaType(javaType);
		return info ;
	}
	
	/**
	 * 获取连接
	 * @param driver
	 * @param url
	 * @param user
	 * @param pwd
	 * @return
	 * @throws Exception
	 */
	public static Connection getConnections(String driver,String url,String user,String pwd){
		Connection conn = null;
		try {
			Properties props = new Properties();
			props.put("remarksReporting", "true");
			props.put("user", user);
			props.put("password", pwd);
			Class.forName(driver);
			conn = DriverManager.getConnection(url, props);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	/**
	 * 其他数据库不需要这个方法 oracle和db2需要
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	private static String getSchema(Connection conn) throws Exception {
		String schema;
		schema = conn.getMetaData().getUserName();
		if ((schema == null) || (schema.length() == 0)) {
			throw new Exception("oracle 数据库模式不允许为空");
		}
		return schema.toUpperCase().toString();
		
	}
	
	/**
	 * 斜杆
	 * @param host
	 * @param port
	 * @param databaseName
	 * @param rdbmsType
	 * @return
	 */
	public static String getRequestUrl(String host,int port,String databaseName,RdbmsType rdbmsType){
		switch (rdbmsType){
			case MYSQL:
				return "jdbc:mysql://"+host+":"+port+"/"+databaseName+"?serverTimezone=GMT%2b8&useUnicode=true&characterEncoding=UTF-8&useSSL=false";
			case SQLSERVER:
				return "jdbc:sqlserver://"+host+":"+port+";DatabaseName="+databaseName;
			case ORACLE:
				return "jdbc:oracle:thin:@//"+host+":"+port+":"+databaseName;
			default:
				return "jdbc:mysql://"+host+":"+port+"/"+databaseName+"?serverTimezone=GMT%2b8&useUnicode=true&characterEncoding=UTF-8&useSSL=false";
		}
	}
	
	/**
	 * 释放对象.
	 * @param rs
	 * @param statement
	 * @param connection
	 */
	public static void close(ResultSet rs,Statement statement,Connection connection){
		if(rs != null){
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if(statement != null){
			try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if(connection != null){
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
