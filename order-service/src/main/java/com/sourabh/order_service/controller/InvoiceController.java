package com.sourabh.order_service.controller;

import com.sourabh.order_service.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER', 'ADMIN')")
    @GetMapping("/{orderUuid}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable String orderUuid,
            HttpServletRequest httpRequest) {

        byte[] pdf = invoiceService.generateInvoice(orderUuid);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice-" + orderUuid + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    @GetMapping("/{orderUuid}/invoice/email")
    public ResponseEntity<String> emailInvoice(@PathVariable String orderUuid) {
        invoiceService.emailInvoice(orderUuid);
        return ResponseEntity.ok("Invoice email requested");
    }

    @GetMapping("/internal/{orderUuid}/invoice")
    public ResponseEntity<byte[]> downloadInvoiceInternal(
            @PathVariable String orderUuid) {
        byte[] pdf = invoiceService.generateInvoice(orderUuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
