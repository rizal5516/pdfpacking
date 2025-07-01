package id.go.bssn.blpid.dto;

import id.go.bssn.blpid.property.SignatureProperty;

import java.util.List;

public class PlaceholderFileRequest {
    private String base64pdf;
    private List<SignatureProperty> signatureProperties;

    public String getBase64pdf() {
        return base64pdf;
    }

    public void setBase64pdf(String base64pdf) {
        this.base64pdf = base64pdf;
    }

    public List<SignatureProperty> getSignatureProperties() {
        return signatureProperties;
    }

    public void setSignatureProperties(List<SignatureProperty> signatureProperties) {
        this.signatureProperties = signatureProperties;
    }
}
