package id.go.bssn.blpid.service;

import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.PdfSigner;
import id.go.bssn.blpid.property.SignatureProperty;
import id.go.bssn.blpid.utils.PdfCmsInjector;
import id.go.bssn.blpid.utils.PdfHashGenerator;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Base64;

@Service
public class PdfPackService {
    public String signBase64Pdf(String base64Pdf, String base64CmsSignature) throws Exception {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        ByteArrayInputStream preSignedStream = new ByteArrayInputStream(pdfBytes);
        ByteArrayOutputStream signedPdf = new ByteArrayOutputStream();

        PdfReader reader = new PdfReader(preSignedStream);
        PdfSigner signer = new PdfSigner(reader, signedPdf, new StampingProperties().useAppendMode());

        String signatureFieldName = generateSecureFieldName();

        signer.signDeferred(signer.getDocument(), signatureFieldName, signedPdf, new PdfCmsInjector(base64CmsSignature));

        return Base64.getEncoder().encodeToString(signedPdf.toByteArray());
    }

    public record HashResponseWithField(PdfHashGenerator.HashResult result, String signatureFieldName) {}

    public HashResponseWithField generateHashOnlyFromBase64(String base64Pdf, SignatureProperty prop) throws Exception {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);

        String signatureFieldName = generateSecureFieldName();
        PdfHashGenerator.HashResult result = PdfHashGenerator.generatePdfHashOnly(inputStream, signatureFieldName, prop);
        return new HashResponseWithField(result, signatureFieldName);
    }

    public String generateSecureFieldName() {
        String raw = String.valueOf(System.currentTimeMillis());
        String base = Base64.getEncoder().encodeToString(raw.getBytes())
                .replaceAll("[^A-Za-z0-9]", "")
                .substring(0, 10);
        return "SIG_" + base;
    }
}
