package id.go.bssn.blpid.v1.service;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.forms.fields.PdfSignatureFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.PdfSigner;
import id.go.bssn.blpid.v1.dto.PackFileRequest;
import id.go.bssn.blpid.v1.dto.PlaceholderFileRequest;
import id.go.bssn.blpid.v1.property.SignatureProperty;
import id.go.bssn.blpid.v1.utils.PdfCmsInjector;
import id.go.bssn.blpid.v1.utils.PdfHashGenerator;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PdfPackService {

    public String signBase64Pdf(String base64Pdf, String base64CmsSignature, String signatureFieldName) throws Exception {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

        // Cek existing signatures
        List<String> existingSignatures = getExistingSignatureFields(pdfBytes);
        boolean hasExistingSignatures = !existingSignatures.isEmpty();

        System.out.println("Signing PDF - Existing signatures: " + existingSignatures.size());
        System.out.println("Using signature field: " + signatureFieldName);

        ByteArrayInputStream preSignedStream = new ByteArrayInputStream(pdfBytes);
        ByteArrayOutputStream signedPdf = new ByteArrayOutputStream();

        try {
            PdfReader reader = new PdfReader(preSignedStream);

            StampingProperties stampingProps = new StampingProperties();

            if (hasExistingSignatures) {
                // Untuk PDF yang sudah ada signature, wajib gunakan append mode dan unethical reading
                reader.setUnethicalReading(true);
                stampingProps.useAppendMode();
            }

            PdfSigner signer = new PdfSigner(reader, signedPdf, stampingProps);

            // PENTING: Gunakan signature field name yang sudah di-generate di placeholder step
            signer.signDeferred(signer.getDocument(), signatureFieldName, signedPdf,
                    new PdfCmsInjector(base64CmsSignature));

            return Base64.getEncoder().encodeToString(signedPdf.toByteArray());

        } catch (Exception e) {
            System.out.println("Signing failed: " + e.getMessage());
            throw new RuntimeException("Failed to sign PDF: " + e.getMessage(), e);
        }
    }

    public record HashResponseWithField(PdfHashGenerator.HashResult result, String signatureFieldName) {}

    public List<String> getExistingSignatureFields(byte[] pdfBytes) {
        List<String> existingFields = new ArrayList<>();

        // Method 1: Try normal reading
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
            PdfReader reader = new PdfReader(inputStream);
            PdfDocument pdfDoc = new PdfDocument(reader);

            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDoc, false);
            if (acroForm != null) {
                Map<String, PdfFormField> fields = acroForm.getFormFields();
                for (Map.Entry<String, PdfFormField> entry : fields.entrySet()) {
                    if (entry.getValue() instanceof PdfSignatureFormField) {
                        existingFields.add(entry.getKey());
                    }
                }
            }
            pdfDoc.close();
            return existingFields;

        } catch (Exception e) {
            System.out.println("Normal reading failed, trying unethical reading: " + e.getMessage());
        }

        // Method 2: Try unethical reading
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
            PdfReader reader = new PdfReader(inputStream);
            reader.setUnethicalReading(true);

            PdfDocument pdfDoc = new PdfDocument(reader);
            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDoc, false);
            if (acroForm != null) {
                Map<String, PdfFormField> fields = acroForm.getFormFields();
                for (Map.Entry<String, PdfFormField> entry : fields.entrySet()) {
                    if (entry.getValue() instanceof PdfSignatureFormField) {
                        existingFields.add(entry.getKey());
                    }
                }
            }
            pdfDoc.close();
            return existingFields;

        } catch (Exception e) {
            System.out.println("Unethical reading also failed: " + e.getMessage());
        }

        // Method 3: Simple byte scanning as last resort
        try {
            String pdfContent = new String(pdfBytes);
            // Look for signature-related keywords in PDF content
            if (pdfContent.contains("/Type/Sig") ||
                    pdfContent.contains("adbe.pkcs7.detached") ||
                    pdfContent.contains("Adobe.PPKLite")) {
                System.out.println("Found signature indicators in PDF content");
                // Return a dummy signature field to indicate presence
                existingFields.add("DETECTED_SIGNATURE_" + System.currentTimeMillis());
            }
        } catch (Exception e) {
            System.out.println("Byte scanning failed: " + e.getMessage());
        }

        return existingFields;
    }

    public HashResponseWithField generateHashOnlyFromBase64(String base64Pdf, SignatureProperty prop) throws Exception {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        String signatureFieldName = generateSecureFieldName();

        // Cek existing signatures
        List<String> existingSignatures = getExistingSignatureFields(pdfBytes);
        boolean hasExistingSignatures = !existingSignatures.isEmpty();

        if (hasExistingSignatures) {
            System.out.println("PDF sudah memiliki " + existingSignatures.size() + " signature(s): " + existingSignatures);
            // Pastikan field name tidak konflik dengan yang sudah ada
            while (existingSignatures.contains(signatureFieldName)) {
                signatureFieldName = generateSecureFieldName();
            }
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);

        try {
            PdfHashGenerator.HashResult result;

            if (hasExistingSignatures) {
                // Langsung gunakan append mode untuk PDF yang sudah ada signature
                result = PdfHashGenerator.generatePdfHashOnlyWithAppend(inputStream, signatureFieldName, prop);
            } else {
                // Gunakan method original untuk PDF fresh
                result = PdfHashGenerator.generatePdfHashOnly(inputStream, signatureFieldName, prop);
            }

            return new HashResponseWithField(result, signatureFieldName);

        } catch (Exception e) {
            System.out.println("Primary method failed: " + e.getMessage());

            // Reset stream untuk fallback
            inputStream.reset();

            // Fallback: selalu gunakan append mode dengan unethical reading
            PdfReader reader = new PdfReader(inputStream);
            reader.setUnethicalReading(true);

            PdfHashGenerator.HashResult result = PdfHashGenerator.generatePdfHashOnlyWithAppend(
                    reader, signatureFieldName, prop);

            return new HashResponseWithField(result, signatureFieldName);
        }
    }

    public String generateSecureFieldName() {
        // Tambahkan randomness lebih untuk avoid collision
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        String raw = timestamp + "_" + random;

        String base = Base64.getEncoder().encodeToString(raw.getBytes())
                .replaceAll("[^A-Za-z0-9]", "");

        // Ambil substring yang lebih panjang untuk reduce collision
        if (base.length() > 15) {
            base = base.substring(0, 15);
        }

        return "SIG_" + base;
    }

    public List<Map<String, Object>> createPlaceholders(List<PlaceholderFileRequest> files) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        for (PlaceholderFileRequest file : files) {
            if (file.getSignatureProperties() == null || file.getSignatureProperties().isEmpty()) {
                throw new IllegalArgumentException("signatureProperties tidak boleh kosong");
            }

            SignatureProperty prop = file.getSignatureProperties().get(0);

            // Cek existing signatures
            byte[] pdfBytes = Base64.getDecoder().decode(file.getBase64pdf());
            List<String> existingSignatures = getExistingSignatureFields(pdfBytes);

            HashResponseWithField hashResult = generateHashOnlyFromBase64(file.getBase64pdf(), prop);

            String base64Hash = Base64.getEncoder().encodeToString(hashResult.result().hash);
            String base64Pdf = Base64.getEncoder().encodeToString(hashResult.result().pdfWithPlaceholder);

            Map<String, Object> res = Map.of(
                    "hash", base64Hash,
                    "pdfWithPlaceholder", base64Pdf,
                    "signatureFieldName", hashResult.signatureFieldName(),
                    "existingSignatures", existingSignatures,
                    "totalSignatures", existingSignatures.size() + 1
            );
            results.add(res);
        }
        return results;
    }

    public List<Map<String, Object>> injectSignatures(List<PackFileRequest> files) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        for (PackFileRequest file : files) {
            String packedPdf = signBase64Pdf(file.getBase64HashPdf(), file.getSignValue(), file.getSignatureFieldName());
            Map<String, Object> res = Map.of(
                    "packedPdf", packedPdf
            );
            results.add(res);
        }
        return results;
    }
}