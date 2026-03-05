package com.sourabh.order_service.controller;

import com.sourabh.order_service.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for generating and distributing order invoices.
 *
 * <p>Provides three capabilities:
 * <ol>
 *   <li>Download a PDF invoice for a given order (buyer / seller / admin).</li>
 *   <li>Email the invoice PDF to the buyer via the user-service.</li>
 *   <li>Internal endpoint for other microservices to fetch raw PDF bytes,
 *       protected by the {@code X-Internal-Secret} header.</li>
 * </ol>
 *
 * <p>Invoices include the full order breakdown: items, pricing (subtotal, tax,
 * discount, delivery fee), buyer and shipping information, and payment status.
 * PDF generation is delegated to {@link InvoiceService}.</p>
 *
 * <p>Base path: {@code /api/order}</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 * @see InvoiceService
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class InvoiceController {

    /** Service responsible for PDF generation and email dispatch. */
    private final InvoiceService invoiceService;

    /**
     * Generates and returns a PDF invoice for the specified order as a
     * downloadable attachment.
     *
     * <p>The response includes {@code Content-Disposition: attachment} so that
     * browsers prompt a file download.</p>
     *
     * @param orderUuid   UUID of the order whose invoice is requested
     * @param httpRequest the current HTTP request (reserved for future use)
     * @return {@link ResponseEntity} containing the PDF byte array with
     *         {@code application/pdf} content type
     */
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

    /**
     * Triggers an asynchronous email delivery of the invoice PDF to the
     * buyer's registered email address.
     *
     * <p>The invoice is generated on the fly and forwarded to the
     * user-service for email dispatch.</p>
     *
     * @param orderUuid UUID of the order whose invoice should be emailed
     * @return {@link ResponseEntity} containing a confirmation message
     */
    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    @GetMapping("/{orderUuid}/invoice/email")
    public ResponseEntity<String> emailInvoice(@PathVariable String orderUuid) {
        invoiceService.emailInvoice(orderUuid);
        return ResponseEntity.ok("Invoice email requested");
    }

    /**
     * Internal-only endpoint used by other microservices to fetch raw PDF
     * invoice bytes.
     *
     * <p>This endpoint bypasses role-based security and is protected
     * exclusively by the {@code X-Internal-Secret} header validated by
     * {@link com.sourabh.order_service.security.InternalSecretInterceptor}.</p>
     *
     * @param orderUuid UUID of the order whose invoice is requested
     * @return {@link ResponseEntity} containing the PDF byte array
     */
    @GetMapping("/internal/{orderUuid}/invoice")
    public ResponseEntity<byte[]> downloadInvoiceInternal(
            @PathVariable String orderUuid) {
        byte[] pdf = invoiceService.generateInvoice(orderUuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
