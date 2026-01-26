package id.go.bssn.blpid.v1.dto;

import id.go.bssn.blpid.v1.property.SignatureProperty;

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
