package core.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.persistence.OptimisticLockException;
import java.util.function.Supplier;

@Component
public class RetryUtil {

    private final static Logger logger = LoggerFactory.getLogger(RetryUtil.class);

    public void retry(String message, int retryMax, int retryDelay, Supplier<Void> call) {
        for (int tryIndex = 0; tryIndex < retryMax; tryIndex++) {
            logger.info("Trying {}: {}", tryIndex, message);
            try{
                call.get();
                break;
            }
            catch(OptimisticLockException e){
                tryIndex++;
                if (tryIndex < retryMax){
                    logger.error("Will retry again. OptimisticLockException received during {}", message, e);
                    //sleep before retry again
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException interruptedException) {
                        logger.error("Failed to sleep before retry. {}", message, e);
                        throw new RuntimeException("Failed to sleep before retry.", e);
                    }
                }
                else{
                    logger.error("Retries over. OptimisticLockException received during {}", message, e);
                    throw e;
                }
            }
        }
    }

}
