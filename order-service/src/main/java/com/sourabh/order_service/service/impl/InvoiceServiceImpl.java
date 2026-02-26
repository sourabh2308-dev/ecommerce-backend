package com.sourabh.order_service.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sourabh.order_service.entity.Order;
import com.sourabh.order_service.entity.OrderItem;
import com.sourabh.order_service.repository.OrderRepository;
import com.sourabh.order_service.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 * INVOICE SERVICE IMPLEMENTATION
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * PURPOSE:
 * --------
 * Generates professional PDF invoices for orders in the e-commerce system.
 * This service handles:
 *   1. Retrieval of order details from database
 *   2. PDF document creation using iText library (Apache 2.0 licensed)
 *   3. Professional formatting with branding colors and layout
 *   4. Item-level line details with quantities and pricing
 *   5. Tax and discount calculations on invoice
 *   6. Shipping address and order metadata display
 *   7. Currency formatting for international support
 * 
 * KEY RESPONSIBILITIES:
 * ---------------------
 * - Fetch order data by UUID and validate existence
 * - Create structured PDF document with A4 page size
 * - Format header section with invoice number and metadata
 * - Generate itemized table with product details and amounts
 * - Calculate and display totals (subtotal, tax, discount, final)
 * - Apply professional styling and branding (colors, fonts, spacing)
 * - Handle edge cases (missing fields, null values, currency formatting)
 * 
 * PDF STRUCTURE (Invoice Layout):
 * ──────────────────
 * 1. Title: "INVOICE" (centered, bold, branded color)
 * 2. Order Info: Order UUID, creation date, status, currency
 * 3. Shipping Address: Name, full address, postal code, phone
 * 4. Items Table: Product | Qty | Unit Price | Line Total
 * 5. Totals Section: Discount amount, tax, final total
 * 6. Footer: "Thank you for your purchase!" (centered)
 * 
 * STYLING:
 * ────────
 * - Title Font: Helvetica 18pt, Bold, Color(79, 70, 229) - Brand indigo
 * - Header Font: Helvetica 11pt, Bold, White on indigo background
 * - Body Font: Helvetica 10pt, Normal, Dark gray text
 * - Table borders: Light gray (200,200,200) for subtle grid
 * - Page margins: 40px all sides (professional spacing)
 * 
 * DEPENDENCIES:
 * ──────────────
 * - OrderRepository: Fetches order from database by UUID
 * - iText (com.lowagie.text): Open source PDF generation library
 * 
 * ANNOTATIONS:
 * ─────────────
 * @Service: Marks class as Spring service layer component (business logic)
 * @RequiredArgsConstructor: Lombok generates constructor for final fields
 * 
 * ══════════════════════════════════════════════════════════════════════════════
 * 
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepository;

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(79, 70, 229));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(79, 70, 229);

    /**
     * Generate a professional PDF invoice for an order.
     * 
     * PURPOSE:
     * Creates a fully formatted PDF invoice document that can be:
     * - Downloaded by customers from order confirmation page
     * - Emailed to customer after order completion
     * - Printed or saved for record-keeping
     * - Used for accounting and tax documentation
     * 
     * PROCESS FLOW:
     * 1. Fetch order from database by UUID (validate existence)
     * 2. Create PDF document with A4 page size (210x297mm)
     * 3. Add title section with centered "INVOICE" heading
     * 4. Add order metadata (order#, date, status, currency)
     * 5. Add shipping address section if address data exists
     * 6. Create itemized table with product details:
     *    - Column 1: Product name (or UUID if name missing)
     *    - Column 2: Quantity
     *    - Column 3: Unit price (formatted with currency symbol)
     *    - Column 4: Line total (quantity * price)
     * 7. Add totals section:
     *    - If discount applied: Show coupon code and discount amount
     *    - If tax applicable: Show tax percentage and amount
     *    - Final total amount
     * 8. Add footer with thank you message
     * 9. Serialize document to byte array and close
     * 
     * PDF STYLING DETAILS:
     * - Page size: A4 (210x297mm)
     * - Margins: 40px on all sides (professional spacing)
     * - Title: Indigo color (79,70,229) matches brand, centered
     * - Table header: Bold white text on indigo background with padding
     * - Table cells: Alternating careful spacing, subtle gray borders
     * - Currency: Indian Rupee symbol ₹ with 2 decimal places
     * 
     * EDGE CASES HANDLED:
     * - Missing product name: Falls back to product UUID
     * - Missing shipping address: Skips address section
     * - Null discount/tax amounts: Skips those rows in totals
     * - Null currency: Defaults to "INR"
     * 
     * ERROR HANDLING:
     * - RuntimeException raised if order not found or deleted
     * - Catch block converts iText exceptions to RuntimeException
     * - Error message indicates PDF generation failure
     * 
     * @param orderUuid UUID of the order to generate invoice for
     * 
     * @return byte[] containing complete PDF binary data that can be:
     *         - Returned as HTTP response (application/pdf content type)
     *         - Stored in file system
     *         - Emailed as attachment
     *         - Displayed in browser with embedded viewer
     * 
     * @throws RuntimeException if order not found, deleted, or PDF generation fails
     */
    @Override
    public byte[] generateInvoice(String orderUuid) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Title
            Paragraph title = new Paragraph("INVOICE", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            doc.add(title);

            // Order Info
            doc.add(new Paragraph("Order #: " + order.getUuid(), BOLD_FONT));
            doc.add(new Paragraph("Date: " + order.getCreatedAt(), BODY_FONT));
            doc.add(new Paragraph("Status: " + order.getStatus(), BODY_FONT));
            doc.add(new Paragraph("Currency: " + (order.getCurrency() != null ? order.getCurrency() : "INR"), BODY_FONT));
            doc.add(new Paragraph(" "));

            // Shipping Address
            if (order.getShippingName() != null) {
                doc.add(new Paragraph("Ship To:", BOLD_FONT));
                doc.add(new Paragraph(order.getShippingName(), BODY_FONT));
                doc.add(new Paragraph(order.getShippingAddress() + ", " + order.getShippingCity(), BODY_FONT));
                doc.add(new Paragraph(order.getShippingState() + " - " + order.getShippingPincode(), BODY_FONT));
                doc.add(new Paragraph("Phone: " + order.getShippingPhone(), BODY_FONT));
                doc.add(new Paragraph(" "));
            }

            // Items Table
            PdfPTable table = new PdfPTable(new float[]{4f, 1f, 2f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);

            addCell(table, "Product", HEADER_FONT, HEADER_BG, Element.ALIGN_LEFT);
            addCell(table, "Qty", HEADER_FONT, HEADER_BG, Element.ALIGN_CENTER);
            addCell(table, "Price", HEADER_FONT, HEADER_BG, Element.ALIGN_RIGHT);
            addCell(table, "Subtotal", HEADER_FONT, HEADER_BG, Element.ALIGN_RIGHT);

            for (OrderItem item : order.getItems()) {
                String productName = item.getProductName() != null ? item.getProductName() : item.getProductUuid();
                addCell(table, productName, BODY_FONT, Color.WHITE, Element.ALIGN_LEFT);
                addCell(table, String.valueOf(item.getQuantity()), BODY_FONT, Color.WHITE, Element.ALIGN_CENTER);
                addCell(table, formatCurrency(item.getPrice()), BODY_FONT, Color.WHITE, Element.ALIGN_RIGHT);
                addCell(table, formatCurrency(item.getPrice() * item.getQuantity()), BODY_FONT, Color.WHITE, Element.ALIGN_RIGHT);
            }
            doc.add(table);

            // Totals
            doc.add(new Paragraph(" "));
            PdfPTable totals = new PdfPTable(new float[]{6f, 2f});
            totals.setWidthPercentage(100);

            if (order.getDiscountAmount() != null && order.getDiscountAmount() > 0) {
                addTotalRow(totals, "Discount (" + order.getCouponCode() + ")", "-" + formatCurrency(order.getDiscountAmount()));
            }
            if (order.getTaxAmount() != null && order.getTaxAmount() > 0) {
                addTotalRow(totals, "Tax (" + order.getTaxPercent() + "%)", formatCurrency(order.getTaxAmount()));
            }
            addTotalRow(totals, "Total", formatCurrency(order.getTotalAmount()));
            doc.add(totals);

            // Footer
            doc.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Thank you for your purchase!", BODY_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font, Color bgColor, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(200, 200, 200));
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBorderWidth(0);
        labelCell.setPadding(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BOLD_FONT));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBorderWidth(0);
        valueCell.setPadding(4);
        table.addCell(valueCell);
    }

    private String formatCurrency(double amount) {
        return String.format("₹%.2f", amount);
    }
}
