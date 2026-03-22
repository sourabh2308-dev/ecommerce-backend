package com.sourabh.order_service.service;

public interface InvoiceService {

    byte[] generateInvoice(String orderUuid);

    void emailInvoice(String orderUuid);
}
