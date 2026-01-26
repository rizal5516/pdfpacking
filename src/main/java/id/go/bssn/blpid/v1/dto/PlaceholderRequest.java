package id.go.bssn.blpid.v1.dto;

import java.util.List;

public class PlaceholderRequest {
    private List<PlaceholderFileRequest> files;

    public List<PlaceholderFileRequest> getFiles() {
        return files;
    }

    public void setFiles(List<PlaceholderFileRequest> files) {
        this.files = files;
    }
}
