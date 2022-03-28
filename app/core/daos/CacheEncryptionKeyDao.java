package core.daos;

import com.workapps.common.core.entities.EncryptionSetting;

import java.util.List;

public interface CacheEncryptionKeyDao {
    List<EncryptionSetting> getAll();
	List<EncryptionSetting> getAll(Integer orgId);
}
