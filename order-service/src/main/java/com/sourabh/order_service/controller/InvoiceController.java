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
 * REST Controller for generating order invoices.
 * 
 * <p>Provides PDF invoice generation for completed orders.
 * Accessible by the buyer, seller, or admin for the specific order.
 * 
 * <p>Invoice includes:
 * <ul>
 *   <li>Order details and items</li>
 *   <li>Pricing breakdown (subtotal, tax, discount, delivery fee)</li>
 *   <li>Buyer and shipping information</li>
 *   <li>Payment status</li>
 * </ul>
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Generates and downloads a PDF invoice for the specified order.
     * 
     * <p>The invoice is generated using iText PDF library and includes
     * all order details, itemized breakdown, and payment information.
     * 
     * <p>Accessible by:
     * <ul>
     *   <li>BUYER: who placed the order</li>
     *   <li>SELLER: who fulfilled the order</li>
     *   <li>ADMIN: for administrative purposes</li>
     * </ul>
     * 
     * @param orderUuid the UUID of the order
     * @param httpRequest the HTTP request (for future extensibility)
     * @return ResponseEntity containing PDF as byte array with appropriate headers
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
     * Send invoice PDF via email to the buyer.
     * This triggers internal logic which generates the document and
     * delegates delivery to the user-service.
     */
    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    @GetMapping("/{orderUuid}/invoice/email")
    public ResponseEntity<String> emailInvoice(@PathVariable String orderUuid) {
        invoiceService.emailInvoice(orderUuid);
        return ResponseEntity.ok("Invoice email requested");
    }

    /**
     * Internal endpoint used by other services to fetch raw PDF bytes.
     * This bypasses role-based security and is protected only by the
     * X-Internal-Secret header handled by InternalSecretFilter.
     */
    @GetMapping("/internal/{orderUuid}/invoice")
    public ResponseEntity<byte[]> downloadInvoiceInternal(
            @PathVariable String orderUuid) {
        byte[] pdf = invoiceService.generateInvoice(orderUuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
