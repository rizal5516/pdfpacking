package id.go.bssn.blpid.dto;

import jakarta.validation.constraints.NotBlank;

public class PdfPackRequest {

    @NotBlank(message = "base64HashPdf tidak boleh kosong")
    private String base64HashPdf;

    @NotBlank(message = "signValue tidak boleh kosong")
    private String signValue;

    public String getBase64HashPdf() {
        return base64HashPdf;
    }

    public void setBase64HashPdf(String base64HashPdf) {
        this.base64HashPdf = base64HashPdf;
    }

    public String getSignValue() {
        return signValue;
    }

    public void setSignValue(String signValue) {
        this.signValue = signValue;
    }
}
