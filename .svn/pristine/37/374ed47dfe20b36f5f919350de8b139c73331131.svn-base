package core.daos.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workapps.common.core.entities.EncryptionSetting;
import core.daos.CacheEncryptionKeyDao;
import core.utils.CacheUtil;
import core.utils.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CacheEncryptionKeyDaoImpl implements CacheEncryptionKeyDao {

    private final ObjectMapper jsonMapper	= new ObjectMapper();


    @Autowired
    private CacheUtil cacheUtil;

    @Value("${redis.ims.data.encryption.key.store}")
    private String dataEncryptionBucket;


    @Override
    public List<EncryptionSetting> getAll() {
        Map<String, String> all = cacheUtil.hgetAll(Enums.DatabaseType.Ims, dataEncryptionBucket);
        return all.values().stream().map(e-> {
            try {
                return jsonMapper.readValue(e, EncryptionSetting.class);
            } catch (IOException ioException) {
                throw new IllegalArgumentException("JSON parse error");
            }
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<EncryptionSetting> getAll(Integer orgId) {
        Map<String, String> all = cacheUtil.hgetAll(Enums.DatabaseType.Ims, dataEncryptionBucket);
        return all.values().stream().map(e-> {
            try {
                return jsonMapper.readValue(e, EncryptionSetting.class);
            } catch (IOException ioException) {
                throw new IllegalArgumentException("JSON parse error");
            }
        }).filter(e -> orgId.equals(e.getOrgId()) && e.isActive()).collect(Collectors.toList());
    }
}
