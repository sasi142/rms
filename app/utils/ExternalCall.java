package utils;


import core.exceptions.InternalServerErrorException;
import core.utils.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ExternalCall {

    final static Logger logger = LoggerFactory.getLogger(ExternalCall.class);

    public <T> T withRetry(Supplier<T> supplier, int count, int delay){
        while(count > 0){
            try {
                return supplier.get();
            }
            catch(Exception e){
                logger.error("Error calling external with retry due to", e);
            }
            count--;
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                logger.error("Thread sleep failing during error calling external with retry due to", e);
            }
        }
        throw new InternalServerErrorException(Enums.ErrorCode.FAILED_WITH_RETRY, "");
    }

}
