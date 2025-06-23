package id.go.bssn.blpid.property;

import jakarta.validation.constraints.NotNull;

public class SignatureProperty {

    @NotNull(message = "visibleSignature tidak boleh null")
    private boolean visibleSignature;
    private Float x;
    private Float y;
    private Float width;
    private Float height;
    private Integer pageNumber;
    private String reason;
    private String location;
    private String imageBase64;
    private String contactInfo;


    public boolean isVisibleSignature() {
        return visibleSignature;
    }

    public void setVisibleSignature(boolean visibleSignature) {
        this.visibleSignature = visibleSignature;
    }

    public Float getX() {
        return x;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float y) {
        this.y = y;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
}
