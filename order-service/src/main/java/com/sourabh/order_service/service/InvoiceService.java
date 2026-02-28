package com.sourabh.order_service.service;

public interface InvoiceService {

    /** Generate a PDF invoice for the given order and return the PDF bytes */
    byte[] generateInvoice(String orderUuid);

    /**
     * Generate invoice PDF and email it to the buyer via the user-service.
     *
     * @param orderUuid the UUID of the order to email an invoice for
     */
    void emailInvoice(String orderUuid);
}
