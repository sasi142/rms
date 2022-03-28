/**
 * 
 */
package core.kms;

public interface KMSService {

    String decrypt(String data, Integer orgId);
	String encrypt(String data, Integer orgId);
}
