package id.go.bssn.blpid.v1.service;

import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.PdfSigner;
import id.go.bssn.blpid.v1.dto.PackFileRequest;
import id.go.bssn.blpid.v1.dto.PlaceholderFileRequest;
import id.go.bssn.blpid.v1.property.SignatureProperty;
import id.go.bssn.blpid.v1.utils.PdfCmsInjector;
import id.go.bssn.blpid.v1.utils.PdfHashGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
public class PdfPackService {

    private static final Logger log = LoggerFactory.getLogger(PdfPackService.class);

    public String signBase64Pdf(String base64Pdf, String base64CmsSignature) throws Exception {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        ByteArrayInputStream preSignedStream = new ByteArrayInputStream(pdfBytes);

        // Output stream terpisah untuk append mode agar tidak corrupt
        ByteArrayOutputStream signedPdfOut = new ByteArrayOutputStream();

        PdfReader reader = new PdfReader(preSignedStream);
        PdfSigner signer = new PdfSigner(reader, signedPdfOut, new StampingProperties().useAppendMode());

        String signatureFieldName = generateSecureFieldName();

        // Gunakan output stream baru untuk signDeferred (bukan stream yang sama dengan PdfSigner)
        ByteArrayOutputStream deferredOut = new ByteArrayOutputStream();
        signer.signDeferred(signer.getDocument(), signatureFieldName, deferredOut, new PdfCmsInjector(base64CmsSignature));

        return Base64.getEncoder().encodeToString(deferredOut.toByteArray());
    }

    public record HashResponseWithField(PdfHashGenerator.HashResult result, String signatureFieldName) {}

    public HashResponseWithField generateHashOnlyFromBase64(String base64Pdf, SignatureProperty prop) throws Exception {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);

        String signatureFieldName = generateSecureFieldName();
        PdfHashGenerator.HashResult result = PdfHashGenerator.generatePdfHashOnly(inputStream, signatureFieldName, prop);
        return new HashResponseWithField(result, signatureFieldName);
    }

    /**
     * Generate nama field signature yang unik dan tidak predictable menggunakan UUID.
     * Sebelumnya pakai System.currentTimeMillis() yang bersifat predictable.
     */
    public String generateSecureFieldName() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "SIG_" + uuid;
    }

    public List<Map<String, Object>> createPlaceholders(List<PlaceholderFileRequest> files) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        for (PlaceholderFileRequest file : files) {
            if (file.getSignatureProperties() == null || file.getSignatureProperties().isEmpty()) {
                throw new IllegalArgumentException("signatureProperties tidak boleh kosong");
            }
            SignatureProperty prop = file.getSignatureProperties().get(0);

            HashResponseWithField hashResult = generateHashOnlyFromBase64(file.getBase64pdf(), prop);

            String base64Hash = Base64.getEncoder().encodeToString(hashResult.result().hash);
            String base64Pdf = Base64.getEncoder().encodeToString(hashResult.result().pdfWithPlaceholder);

            Map<String, Object> res = Map.of(
                    "hash", base64Hash,
                    "pdfWithPlaceholder", base64Pdf,
                    "signatureFieldName", hashResult.signatureFieldName()
            );
            results.add(res);
        }
        return results;
    }

    public List<Map<String, Object>> injectSignatures(List<PackFileRequest> files) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        for (PackFileRequest file : files) {
            String packedPdf = signBase64Pdf(file.getBase64HashPdf(), file.getSignValue());
            results.add(Map.of("packedPdf", packedPdf));
        }
        return results;
    }
}