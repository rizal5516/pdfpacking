package id.go.bssn.blpid.v1.controller;

import id.go.bssn.blpid.v1.dto.*;
import id.go.bssn.blpid.security.PayloadValidator;
import id.go.bssn.blpid.v1.service.PdfPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PdfPackController.class);

    private final PdfPackService packService;
    private final PayloadValidator payloadValidator;

    public PdfPackController(PdfPackService packService, PayloadValidator payloadValidator) {
        this.packService = packService;
        this.payloadValidator = payloadValidator;
    }

    @PostMapping("/placeholder")
    public ResponseEntity<?> generatePlaceholders(@RequestBody PlaceholderRequest request) {
        try {
            if (request.getFiles() == null || request.getFiles().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "files tidak boleh kosong"));
            }

            // Validasi jumlah file & ukuran/format setiap PDF
            payloadValidator.validateFileCount(request.getFiles(), "/placeholder");
            request.getFiles().forEach(file ->
                    payloadValidator.validateIsPdf(file.getBase64pdf(), "base64pdf")
            );

            List<Map<String, Object>> results = packService.createPlaceholders(request.getFiles());
            return ResponseEntity.ok(results);

        } catch (IllegalArgumentException e) {
            // Error validasi input — aman dikembalikan ke client
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // Error internal — log detail, kembalikan pesan generik
            log.error("[ERROR] /placeholder gagal diproses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Terjadi kesalahan saat memproses request. Silakan coba lagi."));
        }
    }

    @PostMapping("/pack")
    public ResponseEntity<?> signMultiple(@RequestBody PackRequest request) {
        long startTime = System.nanoTime();
        try {
            if (request.getFiles() == null || request.getFiles().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "files tidak boleh kosong"));
            }

            // Validasi jumlah file & ukuran/format setiap field
            payloadValidator.validateFileCount(request.getFiles(), "/pack");
            request.getFiles().forEach(file -> {
                payloadValidator.validateIsPdf(file.getBase64HashPdf(), "base64HashPdf");
                payloadValidator.validateBase64Format(file.getSignValue(), "signValue");
            });

            List<Map<String, Object>> results = packService.injectSignatures(request.getFiles());
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            return ResponseEntity.ok(Map.of(
                    "results", results,
                    "durationMs", durationMs
            ));

        } catch (IllegalArgumentException e) {
            // Error validasi input — aman dikembalikan ke client
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "durationMs", durationMs
            ));

        } catch (Exception e) {
            // Error internal — log detail, kembalikan pesan generik
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("[ERROR] /pack gagal diproses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Terjadi kesalahan saat memproses request. Silakan coba lagi.",
                    "durationMs", durationMs
            ));
        }
    }
}