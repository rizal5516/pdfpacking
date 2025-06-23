package id.go.bssn.blpid.controller;

import id.go.bssn.blpid.dto.PdfHashRequest;
import id.go.bssn.blpid.dto.PdfPackRequest;
import id.go.bssn.blpid.dto.PdfPackResponse;
import id.go.bssn.blpid.property.SignatureProperty;
import id.go.bssn.blpid.service.PdfPackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/pdfpack/v1")
@Validated
public class PdfPackController {

    private final PdfPackService packService;

    public PdfPackController(PdfPackService packService) {
        this.packService = packService;
    }

    @PostMapping("/pack")
    public ResponseEntity<PdfPackResponse> signManual(@Valid @RequestBody PdfPackRequest request) {
        long startTime = System.nanoTime();
        try {
            String signedPdfBase64 = packService.signBase64Pdf(
                    request.getBase64HashPdf(),
                    request.getSignValue()
            );

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            return ResponseEntity.ok(new PdfPackResponse(
                    signedPdfBase64,
                    durationMs,
                    "PDF berhasil dipacking"
            ));
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new PdfPackResponse(
                    null,
                    durationMs,
                    "Gagal mempacking: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/placeholder")
    public ResponseEntity<?> generatePdfHashFromBase64(@Valid @RequestBody PdfHashRequest request) {
        try {
            if (request.getSignatureProperties().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "signatureProperties tidak boleh kosong"));
            }

            SignatureProperty prop = request.getSignatureProperties().get(0);

            PdfPackService.HashResponseWithField response = packService.generateHashOnlyFromBase64(request.getBase64pdf(), prop);

            String base64Hash = Base64.getEncoder().encodeToString(response.result().hash);
            String base64Pdf = Base64.getEncoder().encodeToString(response.result().pdfWithPlaceholder);

            return ResponseEntity.ok(Map.of(
                    "hash", base64Hash,
                    "pdfWithPlaceholder", base64Pdf
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
