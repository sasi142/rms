package core.daos;

import java.util.List;

import core.entities.Device;
import core.utils.Enums.NotificationType;

public interface DeviceDao {
	public List<Device> getDevices(List<Integer> recipients, NotificationType type);

	public String deleteDeviceTokens(List<Device> devices);
}
