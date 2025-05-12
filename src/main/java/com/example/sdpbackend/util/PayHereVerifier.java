package com.example.sdpbackend.util;


import com.example.sdpbackend.config.PayHereConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class PayHereVerifier {
    private static final Logger logger = LoggerFactory.getLogger(PayHereVerifier.class);

    @Autowired
    private PayHereConfig payHereConfig;

    /**
     * Generates a hash for payment initialization
     *
     * @param paramMap Map containing all request parameters
     * @return The calculated hash value
     */
    public String generateHash(Map<String, String> paramMap) {
        logger.debug("Generating PayHere hash for payment initialization");

        try {
            // Create a sorted map to ensure parameters are in correct order
            TreeMap<String, String> sortedParams = new TreeMap<>(paramMap);

            // Form the string to hash (merchant_id + order_id + amount + currency + app_id)
            String dataString = sortedParams.get("merchant_id") +
                    sortedParams.get("order_id") +
                    sortedParams.get("amount") +
                    sortedParams.get("currency") +
                    payHereConfig.getAppId();

            logger.debug("String to hash: {}", dataString);

            // Generate HMAC-SHA256 hash
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    payHereConfig.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(dataString.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getEncoder().encodeToString(hashBytes);

            logger.debug("Generated hash: {}", hash);
            return hash;
        } catch (Exception e) {
            logger.error("Error generating PayHere hash: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PayHere hash", e);
        }
    }

    /**
     * Verifies the signature in PayHere notification
     *
     * @param notification Map containing all notification parameters
     * @return true if verification is successful, false otherwise
     */
    public boolean verifyNotification(Map<String, String> notification) {
        logger.debug("Verifying PayHere notification");

        if (notification == null || notification.isEmpty()) {
            logger.warn("Empty notification received");
            return false;
        }

        try {
            String receivedHash = notification.get("md5sig");
            if (receivedHash == null) {
                logger.warn("No md5sig found in notification");
                return false;
            }

            logger.debug("Received hash: {}", receivedHash);

            // For PayHere notification verification, we need to use merchant_id, order_id,
            // payment_id, payhere_amount, payhere_currency, status_code and the app secret
            String merchantId = notification.get("merchant_id");
            String orderId = notification.get("order_id");
            String paymentId = notification.get("payment_id");
            String payhereAmount = notification.get("payhere_amount");
            String payhereCurrency = notification.get("payhere_currency");
            String statusCode = notification.get("status_code");

            // Create the string to hash
            String dataString = merchantId + orderId + paymentId + payhereAmount +
                    payhereCurrency + statusCode + payHereConfig.getAppSecret();

            logger.debug("Data string for verification: {}", dataString);

            // Compute the MD5 hash
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(dataString.getBytes(StandardCharsets.UTF_8));

            // Convert the byte array to a hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String calculatedHash = hexString.toString();
            logger.debug("Calculated hash: {}", calculatedHash);

            // Compare the calculated hash with the received hash
            boolean verified = calculatedHash.equalsIgnoreCase(receivedHash);

            if (!verified) {
                logger.warn("PayHere notification verification failed - hash mismatch");
            } else {
                logger.info("PayHere notification verified successfully");
            }

            return verified;
        } catch (Exception e) {
            logger.error("Error verifying PayHere notification: {}", e.getMessage(), e);
            return false;
        }
    }
}
