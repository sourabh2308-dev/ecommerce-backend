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
import com.sourabh.order_service.dto.InternalUserDto;
import com.sourabh.order_service.dto.InvoiceEmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;

/**
 * Implementation of {@link InvoiceService} that generates branded PDF invoices
 * using the OpenPDF (iText) library and optionally emails them to buyers.
 *
 * <h3>PDF Layout</h3>
 * <ol>
 *   <li>Centred "INVOICE" title in brand indigo (79, 70, 229)</li>
 *   <li>Order metadata: UUID, date, status, currency</li>
 *   <li>Shipping address block (when available)</li>
 *   <li>Itemised table: Product | Qty | Unit Price | Subtotal</li>
 *   <li>Totals section: discount, tax, grand total</li>
 *   <li>Footer: "Thank you for your purchase!"</li>
 * </ol>
 *
 * <p>Email dispatch resolves the buyer’s address via the user-service Feign
 * client and sends the Base64-encoded PDF through
 * {@code UserServiceClient#sendInvoice}.</p>
 *
 * @author Sourabh
 * @version 1.0
 * @since 2026-02-26
 */
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    /** Repository for fetching order data by UUID. */
    private final OrderRepository orderRepository;

    /** Feign client for user-service (email lookup and invoice dispatch). */
    private final com.sourabh.order_service.feign.UserServiceClient userServiceClient;

    /** Title font: Helvetica 18 pt, bold, brand indigo. */
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(79, 70, 229));

    /** Table header font: Helvetica 11 pt, bold, white (for contrast on indigo background). */
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);

    /** Body text font: Helvetica 10 pt, normal weight, dark grey. */
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

    /** Bold body font used for labels and totals. */
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);

    /** Background colour for table header cells (brand indigo). */
    private static final Color HEADER_BG = new Color(79, 70, 229);

    /**
     * {@inheritDoc}
     *
     * <p>Constructs an A4 PDF document (40 px margins) containing order metadata,
     * an optional shipping-address block, an itemised product table, discount/tax
     * totals, and a footer. Falls back to the product UUID when a product name is
     * unavailable, and defaults the currency to {@code INR}.</p>
     *
     * @param orderUuid UUID of the order to invoice
     * @return byte array containing the complete PDF binary
     * @throws RuntimeException if the order is not found or PDF generation fails
     */
    @Override
    public byte[] generateInvoice(String orderUuid) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph("INVOICE", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            doc.add(title);

            doc.add(new Paragraph("Order #: " + order.getUuid(), BOLD_FONT));
            doc.add(new Paragraph("Date: " + order.getCreatedAt(), BODY_FONT));
            doc.add(new Paragraph("Status: " + order.getStatus(), BODY_FONT));
            doc.add(new Paragraph("Currency: " + (order.getCurrency() != null ? order.getCurrency() : "INR"), BODY_FONT));
            doc.add(new Paragraph(" "));

            if (order.getShippingName() != null) {
                doc.add(new Paragraph("Ship To:", BOLD_FONT));
                doc.add(new Paragraph(order.getShippingName(), BODY_FONT));
                doc.add(new Paragraph(order.getShippingAddress() + ", " + order.getShippingCity(), BODY_FONT));
                doc.add(new Paragraph(order.getShippingState() + " - " + order.getShippingPincode(), BODY_FONT));
                doc.add(new Paragraph("Phone: " + order.getShippingPhone(), BODY_FONT));
                doc.add(new Paragraph(" "));
            }

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

    /**
     * {@inheritDoc}
     *
     * <p>Fetches the order, generates a PDF via {@link #generateInvoice},
     * resolves the buyer’s email from user-service, Base64-encodes the PDF,
     * and dispatches it through the Feign client.</p>
     *
     * @param orderUuid UUID of the order whose invoice should be emailed
     * @throws RuntimeException if the order or buyer email is unavailable,
     *                          or the email dispatch fails
     */
    @Override
    public void emailInvoice(String orderUuid) {
        Order order = orderRepository.findByUuidAndIsDeletedFalse(orderUuid)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        byte[] pdf = generateInvoice(orderUuid);

        InternalUserDto user = userServiceClient.getUserByUuid(order.getBuyerUuid());
        if (user == null || user.getEmail() == null) {
            throw new RuntimeException("Buyer email not available");
        }

        String base64 = java.util.Base64.getEncoder().encodeToString(pdf);
        InvoiceEmailRequest req = InvoiceEmailRequest.builder()
                        .orderUuid(orderUuid)
                        .toEmail(user.getEmail())
                        .pdfBase64(base64)
                        .build();
        try {
            userServiceClient.sendInvoice(req);
        } catch (Exception e) {
            throw new RuntimeException("Failed to request invoice email", e);
        }
    }

    /**
     * Creates a styled table cell and appends it to the given table.
     *
     * @param table   target PDF table
     * @param text    cell text content
     * @param font    font to apply
     * @param bgColor background colour
     * @param align   horizontal alignment constant from {@link Element}
     */
    private void addCell(PdfPTable table, String text, Font font, Color bgColor, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(200, 200, 200));
        table.addCell(cell);
    }

    /**
     * Appends a two-column summary row (label + value) to the totals table.
     *
     * @param table target PDF table (2-column layout)
     * @param label left-aligned label text (e.g. "Discount", "Tax", "Total")
     * @param value right-aligned formatted currency value
     */
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

    /**
     * Formats a monetary amount with the Indian Rupee symbol and two decimal places.
     *
     * @param amount numeric amount to format
     * @return formatted string, e.g. "₹123.45"
     */
    private String formatCurrency(double amount) {
        return String.format("₹%.2f", amount);
    }
}
