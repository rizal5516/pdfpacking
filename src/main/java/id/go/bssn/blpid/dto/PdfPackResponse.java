package id.go.bssn.blpid.dto;

public class PdfPackResponse {
    private String base64pdfsigned;
    private long duration;
    private String message;

    public PdfPackResponse(String base64pdfsigned, long duration, String message) {
        this.base64pdfsigned = base64pdfsigned;
        this.duration = duration;
        this.message = message;
    }

    public String getBase64pdfsigned() {
        return base64pdfsigned;
    }

    public long getDuration() {
        return duration;
    }

    public String getMessage() {
        return message;
    }

}
