package com.sourabh.order_service.service;

/**
 * Service interface for generating and distributing PDF invoices.
 *
 * <p>Invoices are created using the iText (OpenPDF) library and contain order
 * metadata, an itemised product table, tax/discount breakdowns, shipping
 * address, and a branded header/footer.</p>
 *
 * <p>Email distribution delegates to the user-service via an OpenFeign client
 * to resolve buyer email addresses and dispatch the invoice attachment.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
public interface InvoiceService {

    /**
     * Generates a PDF invoice for the specified order.
     *
     * <p>The returned byte array contains the complete PDF document suitable for
     * streaming as an HTTP response ({@code application/pdf}), storing on disk,
     * or attaching to an email.</p>
     *
     * @param orderUuid UUID of the order to invoice
     * @return byte array containing the rendered PDF
     * @throws RuntimeException if the order is not found or PDF generation fails
     */
    byte[] generateInvoice(String orderUuid);

    /**
     * Generates the invoice PDF and emails it to the buyer.
     *
     * <p>The buyer's email address is resolved through the user-service Feign
     * client. The PDF is Base64-encoded and sent as an attachment via
     * {@code UserServiceClient#sendInvoice}.</p>
     *
     * @param orderUuid UUID of the order whose invoice should be emailed
     * @throws RuntimeException if the order is not found, the buyer email is
     *                          unavailable, or the email dispatch fails
     */
    void emailInvoice(String orderUuid);
}
