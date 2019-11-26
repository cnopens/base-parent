package com.application.demo.task;

import com.application.base.rest.api.KylinRestSession;
import com.application.base.rest.factory.KylinJestSessionFactory;
import com.application.demo.bean.CubeInfo;
import com.application.demo.cont.KylinConstant;
import com.application.demo.init.CubeInfoProvider;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * @author:孤狼
 * @NAME: CubesInfoTask
 * @DESC: 获得所有的cube信息
 **/

@Component
@JobHandler(value="cubesJobHandler")
@Slf4j
public class CubesInfoTask extends IJobHandler {
	
	@Autowired
	private KylinJestSessionFactory sessionFactory;
	
	@Autowired
	private CubeInfoProvider infoProvider;

	
	@Override
	public ReturnT<String> execute(String param) throws Exception {
		int offset=0,limit =5000;
		KylinRestSession restSession = sessionFactory.getRestSession();
		String json = restSession.listCubes(offset,limit,null,null);
		ArrayList<CubeInfo> cubeInfos = infoProvider.getCubesInfo(json);
		if (!cubeInfos.isEmpty()){
			infoProvider.put(KylinConstant.COBE_ARRAY,cubeInfos);
		}
		return new ReturnT<>(ReturnT.SUCCESS_CODE,"返回成功!");
	}
}