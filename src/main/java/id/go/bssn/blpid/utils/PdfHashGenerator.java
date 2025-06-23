package id.go.bssn.blpid.utils;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.signatures.*;
import id.go.bssn.blpid.property.SignatureProperty;

import java.io.*;
import java.security.MessageDigest;
import java.util.Base64;

public class PdfHashGenerator {

    public static class HashResult {
        public byte[] pdfWithPlaceholder;
        public byte[] hash;
    }

    public static class HashCaptureSignatureContainer implements IExternalSignatureContainer {
        private byte[] dataToSign;

        public byte[] getDataToSign() {
            return dataToSign;
        }

        @Override
        public byte[] sign(InputStream data) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = data.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                dataToSign = baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read data to sign", e);
            }
            return new byte[0];
        }

        @Override
        public void modifySigningDictionary(PdfDictionary dic) {
            dic.put(PdfName.Filter, PdfName.Adobe_PPKLite);
            dic.put(PdfName.SubFilter, PdfName.Adbe_pkcs7_detached);
        }
    }

    public static HashResult generateHash(String inputPdfPath, String signatureFieldName, SignatureProperty prop) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(inputPdfPath);
        PdfSigner signer = new PdfSigner(reader, baos, new StampingProperties());

        signer.setFieldName(signatureFieldName);

        if (Boolean.TRUE.equals(prop.isVisibleSignature())) {
            if (prop.getImageBase64() == null || prop.getImageBase64().isEmpty()) {
                throw new IllegalArgumentException("imageBase64 wajib diisi jika visibleSignature = true");
            }

            Rectangle rect = new Rectangle(400, 50, 150, 80);
            if (prop.getX() != null && prop.getY() != null && prop.getWidth() != null && prop.getHeight() != null) {
                rect = new Rectangle(prop.getX(), prop.getY(), prop.getWidth(), prop.getHeight());
            }

            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReuseAppearance(false);
            appearance.setPageRect(rect);
            appearance.setPageNumber(prop.getPageNumber() != null ? prop.getPageNumber() : 1);

            if (prop.getReason() != null) {
                appearance.setReason(prop.getReason());
            }

            if (prop.getLocation() != null) {
                appearance.setLocation(prop.getLocation());
            }

            if (prop.getContactInfo() != null) {
                appearance.setContact(prop.getContactInfo());
            }

            if (prop.getImageBase64() != null && !prop.getImageBase64().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(prop.getImageBase64());
                    ImageData image = ImageDataFactory.create(imageBytes);
                    appearance.setSignatureGraphic(image);
                    appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
                    HashCaptureSignatureContainer container = new HashCaptureSignatureContainer();
                    signer.signExternalContainer(container, 8192);

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(container.getDataToSign());

                    HashResult result = new HashResult();
                    result.pdfWithPlaceholder = baos.toByteArray();
                    result.hash = hash;

                    return result;
                } catch (Exception e) {
                    throw new RuntimeException("Gagal decode imageBase64 untuk signatureGraphic", e);
                }
            }

            appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
        }

        HashCaptureSignatureContainer container = new HashCaptureSignatureContainer();
        signer.signExternalContainer(container, 8192);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(container.getDataToSign());

        HashResult result = new HashResult();
        result.pdfWithPlaceholder = baos.toByteArray();
        result.hash = hash;

        return result;
    }

    public static HashResult generatePdfHashOnly(InputStream pdfInputStream, String signatureFieldName, SignatureProperty prop) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(pdfInputStream);
        PdfSigner signer = new PdfSigner(reader, baos, new StampingProperties());

        signer.setFieldName(signatureFieldName);

        if (Boolean.TRUE.equals(prop.isVisibleSignature())) {
            if (prop.getImageBase64() == null || prop.getImageBase64().isEmpty()) {
                throw new IllegalArgumentException("imageBase64 wajib diisi jika visibleSignature = true");
            }

            Rectangle rect = new Rectangle(400, 50, 150, 80);
            if (prop.getX() != null && prop.getY() != null && prop.getWidth() != null && prop.getHeight() != null) {
                rect = new Rectangle(prop.getX(), prop.getY(), prop.getWidth(), prop.getHeight());
            }

            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReuseAppearance(false);
            appearance.setPageRect(rect);
            appearance.setPageNumber(prop.getPageNumber() != null ? prop.getPageNumber() : 1);

            if (prop.getReason() != null) appearance.setReason(prop.getReason());
            if (prop.getLocation() != null) appearance.setLocation(prop.getLocation());
            if (prop.getContactInfo() != null) appearance.setContact(prop.getContactInfo());

            try {
                byte[] imageBytes = Base64.getDecoder().decode(prop.getImageBase64());
                ImageData image = ImageDataFactory.create(imageBytes);
                appearance.setSignatureGraphic(image);
                appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
            } catch (Exception e) {
                throw new RuntimeException("Gagal decode imageBase64 untuk signatureGraphic", e);
            }
        } else {
            signer.getSignatureAppearance().setRenderingMode(PdfSignatureAppearance.RenderingMode.DESCRIPTION);
        }

        HashCaptureSignatureContainer container = new HashCaptureSignatureContainer();
        signer.signExternalContainer(container, 8192);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(container.getDataToSign());

        HashResult result = new HashResult();
        result.hash = hash;
        result.pdfWithPlaceholder = baos.toByteArray();
        return result;
    }
}
