package id.go.bssn.blpid.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Component
public class PayloadValidator {

    private static final Logger log = LoggerFactory.getLogger(PayloadValidator.class);

    @Value("${app.max-files-per-request:10}")
    private int maxFilesPerRequest;

    @Value("${app.max-base64-size-chars:14000000}")
    private int maxBase64SizeChars;

    public void validateFileCount(List<?> files, String endpoint) {
        if (files.size() > maxFilesPerRequest) {
            log.warn("[VALIDATION] {} menolak request: jumlah file {} melebihi batas {}",
                    endpoint, files.size(), maxFilesPerRequest);
            throw new IllegalArgumentException(
                    String.format("Jumlah file (%d) melebihi batas maksimum (%d) per request",
                            files.size(), maxFilesPerRequest)
            );
        }
    }

    public void validateBase64Size(String base64, String fieldName) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException(fieldName + " tidak boleh kosong");
        }
        if (base64.length() > maxBase64SizeChars) {
            log.warn("[VALIDATION] Field '{}' melebihi batas ukuran: {} karakter (maks: {})",
                    fieldName, base64.length(), maxBase64SizeChars);
            throw new IllegalArgumentException(
                    String.format("Ukuran %s (%d karakter) melebihi batas maksimum (%d karakter)",
                            fieldName, base64.length(), maxBase64SizeChars)
            );
        }
    }

    public void validateBase64Format(String base64, String fieldName) {
        validateBase64Size(base64, fieldName);
        try {
            Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            log.warn("[VALIDATION] Field '{}' bukan format Base64 yang valid", fieldName);
            throw new IllegalArgumentException(fieldName + " bukan format Base64 yang valid");
        }
    }

    public void validateIsPdf(String base64, String fieldName) {
        validateBase64Format(base64, fieldName);
        byte[] bytes = Base64.getDecoder().decode(base64);
        if (bytes.length < 4 ||
                bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            log.warn("[VALIDATION] Field '{}' bukan file PDF yang valid (magic bytes salah)", fieldName);
            throw new IllegalArgumentException(fieldName + " bukan file PDF yang valid");
        }
    }
}