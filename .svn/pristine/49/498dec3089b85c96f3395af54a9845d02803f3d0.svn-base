package core.services;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import core.daos.DeviceDao;
import core.entities.Device;

@Service
public class DeviceServiceImpl implements DeviceService, InitializingBean {
	
	@Autowired
	private DeviceDao deviceDao;

	@Async
	@Override
	public void deleteDeviceTokens(List<Device> devices) {
		deviceDao.deleteDeviceTokens(devices);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
	}
}
