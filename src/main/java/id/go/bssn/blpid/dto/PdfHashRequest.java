package id.go.bssn.blpid.dto;

import id.go.bssn.blpid.property.SignatureProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class PdfHashRequest {

    @NotBlank(message = "base64pdf tidak boleh kosong")
    private String base64pdf;

    @NotNull(message = "signatureProperties tidak boleh null")
    private List<@Valid SignatureProperty> signatureProperties;

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
