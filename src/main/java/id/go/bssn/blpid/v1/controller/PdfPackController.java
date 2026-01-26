package id.go.bssn.blpid.v1.controller;

import id.go.bssn.blpid.v1.dto.*;
import id.go.bssn.blpid.v1.dto.PackRequest;
import id.go.bssn.blpid.v1.dto.PlaceholderRequest;
import id.go.bssn.blpid.v1.service.PdfPackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pdfpack/v1")
@Validated
public class PdfPackController {

    private final PdfPackService packService;

    public PdfPackController(PdfPackService packService) {
        this.packService = packService;
    }

    // SINGLE & MULTIPLE PLACEHOLDER
    @PostMapping("/placeholder")
    public ResponseEntity<?> generatePlaceholders(@RequestBody PlaceholderRequest request) {
        try {
            if (request.getFiles() == null || request.getFiles().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "files tidak boleh kosong"));
            }
            List<Map<String, Object>> results = packService.createPlaceholders(request.getFiles());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // SINGLE & MULTIPLE SIGNING
    @PostMapping("/pack")
    public ResponseEntity<?> signMultiple(@RequestBody PackRequest request) {
        long startTime = System.nanoTime();
        try {
            if (request.getFiles() == null || request.getFiles().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "files tidak boleh kosong"));
            }
            List<Map<String, Object>> results = packService.injectSignatures(request.getFiles());
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            return ResponseEntity.ok(Map.of(
                    "results", results,
                    "durationMs", durationMs
            ));
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", e.getMessage(),
                    "durationMs", durationMs
            ));
        }
    }
}
