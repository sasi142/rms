package core.encryption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.daos.CacheEncryptionKeyDao;
import com.workapps.common.core.entities.EncryptionSetting;
import com.workapps.common.core.services.DataEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Component
public class EncryptionLoader implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(EncryptionLoader.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private CacheEncryptionKeyDao cacheEncryptionKeyDao;

    @Autowired
    private DataEncryptionService dataEncryptionService;

    @Autowired
    @Qualifier("scheduler")
    private TaskScheduler taskScheduler;

	@Override
	public void afterPropertiesSet() throws Exception{
        List<EncryptionSetting> encryptionSettings = cacheEncryptionKeyDao.getAll();
        logger.info("Initializing with encryption settings {}", encryptionSettings);
        try {
            dataEncryptionService.init(encryptionSettings);
        } catch (Exception e) {
            logger.error("Failed to initialize the data  encryption service due to", e);
            throw new RuntimeException("Data encryption initialization failed", e);
        }
        try {
            initializeReloadOnKeyRotation(encryptionSettings);
        } catch (Exception e) {
            logger.error("Failed to initialize key rotation due to", e);
            throw new RuntimeException("Data encryption rotation setup failed", e);
        }
    }

    private void initializeReloadOnKeyRotation(List<EncryptionSetting> encryptionSettings) throws IOException {
        Map<Integer, String> schedules = readSchedules(encryptionSettings);
        if (!schedules.isEmpty()){
            for (Map.Entry<Integer, String> entry : schedules.entrySet()) {
                logger.info("Setting schedule for org {} with cron {}", entry.getKey(), entry.getValue());
                taskScheduler.schedule(() -> regenerate(entry.getKey()), new CronTrigger(entry.getValue()));
            }
        }
    }

    private void regenerate(int orgId){
        if (!localInSync(orgId)) {
            logger.info("Found local not in sync for org {}. Reloading ", orgId);
            reload();
            return;
        }

        //start a single thread for continuous check for sometime
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            int maxRun = 100;
            int run = 1;
            while(run < maxRun){
                run++;
                logger.info("Trying to regenerate {} again for org {} ", run, orgId);
                if (!localInSync(orgId)) {
                    logger.info("Found local not in sync for org {}. Reloading ", orgId);
                    reload();
                    break;
                }
                try{
                    Thread.sleep(1000);
                }
                catch(InterruptedException e){
                    logger.error("Thread interrupted  while processing for org {} due to", orgId, e);
                    break;
                }
            }
        }, 2, TimeUnit.SECONDS);
    }

    public void reload() {
        List<EncryptionSetting> encryptionSettings = cacheEncryptionKeyDao.getAll();
        logger.info("Initializing with encryption settings {}", encryptionSettings);
        try {
            dataEncryptionService.init(encryptionSettings);
        } catch (Exception e) {
            logger.error("Failed to schedule task due to", e);
            throw new RuntimeException("Data encryption reloading failed", e);
        }
    }

    private boolean localInSync(Integer orgId) {
        List<EncryptionSetting> orgEncSettings = cacheEncryptionKeyDao.getAll(orgId);
        Optional<EncryptionSetting> active = orgEncSettings.stream().filter(EncryptionSetting::isActive).findAny();
        if (active.isPresent()) {
            //we have active key in cache
            //check active key present in local cache of dataEncryptionService
            if (dataEncryptionService.isEnabled(orgId)) {
                //present. check if changed
            	logger.info("Active Encryption Id: {} & local Cache Encryption Id: {}", active.get().getDataEncryptionKeyId(),dataEncryptionService.key(orgId));
                return active.get().getDataEncryptionKeyId().equals(dataEncryptionService.key(orgId));
            } else {
                //not present. we have new, not in sync, return false
                return false;
            }
        }
        //no active key present, no generation
        throw new RuntimeException("Allowed only for active key setup");
    }

    private Map<Integer, String> readSchedules(List<EncryptionSetting> encryptionSettings) throws IOException {
        Map<Integer, String> schedulerOrgIdProvider = new HashMap<>();
        List<EncryptionSetting> activeSettings = encryptionSettings.stream().filter(EncryptionSetting::isActive).collect(Collectors.toList());

        logger.info("Found active settings {}", activeSettings);

        for (EncryptionSetting activeSetting : activeSettings) {
            JsonNode providerSetting = mapper.readTree(activeSetting.getApplicationSetting());
            boolean rotating = providerSetting.has("rotating") && providerSetting.get("rotating").asBoolean();
            if (rotating && providerSetting.has("rotateKeyScheduler")) {
                schedulerOrgIdProvider.put(activeSetting.getOrgId(), providerSetting.get("rotateKeyScheduler").asText().trim());
            }
        }

        logger.info("Active org Id Scheduler {}", schedulerOrgIdProvider);
        return schedulerOrgIdProvider;
    }
}
