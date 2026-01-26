package id.go.bssn.blpid.v1.dto;

public class PackFileRequest {
    private String base64HashPdf;
    private String signValue;
    private String signatureFieldName; // Tambahkan field ini

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

    public String getSignatureFieldName() {
        return signatureFieldName;
    }

    public void setSignatureFieldName(String signatureFieldName) {
        this.signatureFieldName = signatureFieldName;
    }
}