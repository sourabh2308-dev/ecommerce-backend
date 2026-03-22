package com.sourabh.product_service.service;

import com.sourabh.product_service.dto.request.CreateProductRequest;
import com.sourabh.product_service.dto.request.UpdateProductRequest;
import com.sourabh.product_service.dto.response.ProductResponse;
import com.sourabh.product_service.entity.Product;
import com.sourabh.product_service.entity.ProductStatus;
import com.sourabh.product_service.exception.ProductNotFoundException;
import com.sourabh.product_service.exception.ProductStateException;
import com.sourabh.product_service.exception.UnauthorizedProductAccessException;
import com.sourabh.product_service.repository.ProductRepository;
import com.sourabh.product_service.search.service.ProductSearchService;
import com.sourabh.product_service.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl Unit Tests")
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductSearchService productSearchService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product activeProduct;

    private ArgumentCaptor<Product> productCaptor;

    @BeforeEach
    void setUp() {
        activeProduct = Product.builder()
                .uuid("prod-uuid")
                .name("Test Product")
                .description("A test product")
                .price(99.99)
                .stock(10)
                .category("Electronics")
                .sellerUuid("seller-1")
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();

        productCaptor = ArgumentCaptor.forClass(Product.class);
    }

    @Test
    @DisplayName("createProduct: saved in DRAFT status and indexed in Elasticsearch")
    void createProduct_savedAsDraftAndIndexed() {
        CreateProductRequest req = new CreateProductRequest();
        req.setName("Widget");
        req.setDescription("A widget");
        req.setPrice(10.0);
        req.setStock(100);
        req.setCategory("Tools");

        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = productService.createProduct(req, "seller-1");

        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(productCaptor.getValue().getSellerUuid()).isEqualTo("seller-1");
        assertThat(response.getName()).isEqualTo("Widget");
        verify(productSearchService).indexProductByUuid(any());
    }

    @Test
    @DisplayName("approveProduct: transitions DRAFT to ACTIVE")
    void approveProduct_setsStatusToActive() {
        activeProduct.setStatus(ProductStatus.DRAFT);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.approveProduct("prod-uuid");

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        verify(productRepository).save(activeProduct);
    }

    @Test
    @DisplayName("approveProduct: throws when product not found")
    void approveProduct_notFound_throwsProductNotFoundException() {
        when(productRepository.findByUuidAndIsDeletedFalse("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.approveProduct("ghost"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("blockProduct: sets status to BLOCKED")
    void blockProduct_setsStatusToBlocked() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.blockProduct("prod-uuid");

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.BLOCKED);
    }

    @Test
    @DisplayName("unblockProduct: BLOCKED → DRAFT (requires re-approval)")
    void unblockProduct_blockedToDraft() {
        activeProduct.setStatus(ProductStatus.BLOCKED);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.unblockProduct("prod-uuid");

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }

    @Test
    @DisplayName("unblockProduct: non-BLOCKED product throws ProductStateException")
    void unblockProduct_notBlocked_throwsProductStateException() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.unblockProduct("prod-uuid"))
                .isInstanceOf(ProductStateException.class)
                .hasMessageContaining("not blocked");
    }

    @Test
    @DisplayName("updateProduct: seller updates own product")
    void updateProduct_sellerUpdatesOwnProduct() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateProductRequest req = new UpdateProductRequest();
        req.setName("Updated Widget");
        req.setPrice(149.99);

        productService.updateProduct("prod-uuid", req, "SELLER", "seller-1");

        assertThat(activeProduct.getName()).isEqualTo("Updated Widget");
        assertThat(activeProduct.getPrice()).isEqualTo(149.99);
        verify(productSearchService).indexProductByUuid("prod-uuid");
    }

    @Test
    @DisplayName("updateProduct: seller cannot update another seller's product")
    void updateProduct_sellerOtherProduct_throwsUnauthorized() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));

        UpdateProductRequest req = new UpdateProductRequest();
        req.setName("Hack");

        assertThatThrownBy(() -> productService.updateProduct("prod-uuid", req, "SELLER", "other-seller"))
                .isInstanceOf(UnauthorizedProductAccessException.class)
                .hasMessageContaining("your own product");
    }

    @Test
    @DisplayName("updateProduct: seller cannot update BLOCKED product")
    void updateProduct_blockedProduct_throwsProductStateException() {
        activeProduct.setStatus(ProductStatus.BLOCKED);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.updateProduct("prod-uuid", new UpdateProductRequest(), "SELLER", "seller-1"))
                .isInstanceOf(ProductStateException.class)
                .hasMessageContaining("Blocked");
    }

    @Test
    @DisplayName("updateProduct: stock=0 transitions to OUT_OF_STOCK")
    void updateProduct_zeroStock_setsOutOfStock() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateProductRequest req = new UpdateProductRequest();
        req.setStock(0);

        productService.updateProduct("prod-uuid", req, "SELLER", "seller-1");

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("updateProduct: positive stock restores OUT_OF_STOCK to ACTIVE")
    void updateProduct_positiveStockOnOutOfStock_restoresActive() {
        activeProduct.setStatus(ProductStatus.OUT_OF_STOCK);
        activeProduct.setStock(0);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateProductRequest req = new UpdateProductRequest();
        req.setStock(5);

        productService.updateProduct("prod-uuid", req, "SELLER", "seller-1");

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("softDeleteProduct: admin deletes any product")
    void softDeleteProduct_adminDeletesAny() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.softDeleteProduct("prod-uuid", "ADMIN", "admin-uuid");

        assertThat(activeProduct.getIsDeleted()).isTrue();
        verify(productSearchService).removeProductFromIndex("prod-uuid");
    }

    @Test
    @DisplayName("softDeleteProduct: seller deletes own product")
    void softDeleteProduct_sellerDeletesOwn() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.softDeleteProduct("prod-uuid", "SELLER", "seller-1");

        assertThat(activeProduct.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("softDeleteProduct: seller cannot delete another seller's product")
    void softDeleteProduct_sellerOtherProduct_throwsUnauthorized() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.softDeleteProduct("prod-uuid", "SELLER", "other-seller"))
                .isInstanceOf(UnauthorizedProductAccessException.class)
                .hasMessageContaining("your own product");
    }

    @Test
    @DisplayName("getProductByUuid: BUYER can see ACTIVE product")
    void getProductByUuid_buyerSeesActiveProduct() {
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));

        ProductResponse response = productService.getProductByUuid("prod-uuid", "BUYER");

        assertThat(response.getUuid()).isEqualTo("prod-uuid");
    }

    @Test
    @DisplayName("getProductByUuid: BUYER cannot see BLOCKED product")
    void getProductByUuid_buyerBlockedProduct_throwsProductNotFoundException() {
        activeProduct.setStatus(ProductStatus.BLOCKED);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.getProductByUuid("prod-uuid", "BUYER"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("getProductByUuid: ADMIN can see BLOCKED product")
    void getProductByUuid_adminSeesBlockedProduct() {
        activeProduct.setStatus(ProductStatus.BLOCKED);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));

        ProductResponse response = productService.getProductByUuid("prod-uuid", "ADMIN");

        assertThat(response.getUuid()).isEqualTo("prod-uuid");
    }

    @Test
    @DisplayName("reduceStock: decrements stock correctly")
    void reduceStock_decrementsStock() {
        activeProduct.setStock(10);
        when(productRepository.findByUuidAndIsDeletedFalseForUpdate("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.reduceStock("prod-uuid", 3);

        assertThat(activeProduct.getStock()).isEqualTo(7);
        verify(productRepository).save(activeProduct);
    }

    @Test
    @DisplayName("reduceStock: stock reaches 0 → OUT_OF_STOCK")
    void reduceStock_zeroStock_setsOutOfStock() {
        activeProduct.setStock(5);
        when(productRepository.findByUuidAndIsDeletedFalseForUpdate("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.reduceStock("prod-uuid", 5);

        assertThat(activeProduct.getStock()).isEqualTo(0);
        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("reduceStock: insufficient stock throws ProductStateException")
    void reduceStock_insufficientStock_throwsProductStateException() {
        activeProduct.setStock(2);
        when(productRepository.findByUuidAndIsDeletedFalseForUpdate("prod-uuid")).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.reduceStock("prod-uuid", 5))
                .isInstanceOf(ProductStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("reduceStock: non-ACTIVE product throws ProductStateException")
    void reduceStock_nonActiveProduct_throwsProductStateException() {
        activeProduct.setStatus(ProductStatus.BLOCKED);
        when(productRepository.findByUuidAndIsDeletedFalseForUpdate("prod-uuid")).thenReturn(Optional.of(activeProduct));

        assertThatThrownBy(() -> productService.reduceStock("prod-uuid", 1))
                .isInstanceOf(ProductStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("reduceStock: product not found throws ProductNotFoundException")
    void reduceStock_notFound_throwsProductNotFoundException() {
        when(productRepository.findByUuidAndIsDeletedFalseForUpdate("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.reduceStock("ghost", 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("restoreStock: increments stock correctly")
    void restoreStock_incrementsStock() {
        activeProduct.setStock(3);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.restoreStock("prod-uuid", 4);

        assertThat(activeProduct.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("restoreStock: OUT_OF_STOCK product becomes ACTIVE when stock restored")
    void restoreStock_outOfStock_becomesActive() {
        activeProduct.setStatus(ProductStatus.OUT_OF_STOCK);
        activeProduct.setStock(0);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.restoreStock("prod-uuid", 2);

        assertThat(activeProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("updateRating: first review sets correct average")
    void updateRating_firstReview_setsAverage() {
        activeProduct.setTotalReviews(0);
        activeProduct.setAverageRating(0.0);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.updateRating("prod-uuid", 4);

        assertThat(activeProduct.getAverageRating()).isEqualTo(4.0);
        assertThat(activeProduct.getTotalReviews()).isEqualTo(1);
    }

    @Test
    @DisplayName("updateRating: second review computes correct running average")
    void updateRating_secondReview_correctRunningAverage() {
        activeProduct.setTotalReviews(1);
        activeProduct.setAverageRating(4.0);
        when(productRepository.findByUuidAndIsDeletedFalse("prod-uuid")).thenReturn(Optional.of(activeProduct));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        productService.updateRating("prod-uuid", 2); 

        assertThat(activeProduct.getAverageRating()).isEqualTo(3.0);
        assertThat(activeProduct.getTotalReviews()).isEqualTo(2);
    }
}
