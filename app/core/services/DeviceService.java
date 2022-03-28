package core.services;

import java.util.List;

import core.entities.Device;

public interface DeviceService {
	public void deleteDeviceTokens(List<Device> devices);
}
