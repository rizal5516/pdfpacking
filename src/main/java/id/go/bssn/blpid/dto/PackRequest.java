package id.go.bssn.blpid.dto;

import java.util.List;

public class PackRequest {
    private List<PackFileRequest> files;

    public List<PackFileRequest> getFiles() {
        return files;
    }

    public void setFiles(List<PackFileRequest> files) {
        this.files = files;
    }
}
