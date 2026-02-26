package com.sourabh.order_service.service;

public interface InvoiceService {

    /** Generate a PDF invoice for the given order and return the PDF bytes */
    byte[] generateInvoice(String orderUuid);
}
