package id.go.bssn.blpid.dto;

public class PackFileRequest {
    private String base64HashPdf;
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
