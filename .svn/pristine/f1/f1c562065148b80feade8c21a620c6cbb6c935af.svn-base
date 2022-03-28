package core.utils;

import core.redis.RedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class CacheUtil {

    private static final Logger logger	= LoggerFactory.getLogger(CacheUtil.class);

    @Autowired
    private RedisConnection redisConnection;

    public String get(String bucket, Object key) {
        Jedis jedis = redisConnection.getSlaveConnection(Enums.DatabaseType.Ims);
        try {
            return jedis.hget(bucket, String.valueOf(key));
        } finally {
            redisConnection.releaseSlaveConnection(jedis, Enums.DatabaseType.Ims);
        }
    }

    public void put(String bucket, Object key, Object value) {
        Jedis jedis = redisConnection.getSlaveConnection(Enums.DatabaseType.Ims);
        try {
            jedis.hset(bucket, String.valueOf(key), String.valueOf(value));
        } finally {
            redisConnection.releaseSlaveConnection(jedis, Enums.DatabaseType.Ims);
        }
    }

    public String get(Enums.DatabaseType db, String prefix, Object key) {
        Jedis jedis = redisConnection.getSlaveConnection(db);
        try {
            return jedis.get(prefix + key);
        } finally {
            redisConnection.releaseSlaveConnection(jedis, db);
        }
    }

    public void put(Enums.DatabaseType db, String prefix, Object key, Object value, int expireInMillis) {
        Jedis jedis = redisConnection.getSlaveConnection(db);
        try {
            SetParams setParams = new SetParams();
            setParams.px(expireInMillis);
            jedis.set(prefix + key, String.valueOf(value), setParams);
        } finally {
            redisConnection.releaseSlaveConnection(jedis, db);
        }
    }

    public Map<String, String> hgetAll(Enums.DatabaseType db, String bucket) {
        Jedis jedis = redisConnection.getSlaveConnection(db);
        try {
            return jedis.hgetAll(bucket);
        } finally {
            redisConnection.releaseSlaveConnection(jedis, db);
        }
    }

    public Map<String, String> hmget(Enums.DatabaseType db, String bucket, String... keys) {
        Jedis jedis = redisConnection.getSlaveConnection(db);
        try {
            List<String> data = jedis.hmget(bucket, keys);
            Map<String, String> result = new HashMap<>();
            for (int i = 0; i < keys.length; i++) {
                result.put(keys[i], data.get(i));
            }
            return result;
        } finally {
            redisConnection.releaseSlaveConnection(jedis, db);
        }
    }

    public void pipeline(String message, Enums.DatabaseType db, Consumer<Pipeline> consumer){
        logger.info("Start: {}", message);
        Jedis jedis = redisConnection.getMasterConnection(db);
        try {
            Pipeline p = jedis.pipelined();
            consumer.accept(p);
            p.sync();
        } finally {
            redisConnection.releaseMasterConnection(jedis, db);
        }
        logger.info("Completed: {}", message);
    }
}
