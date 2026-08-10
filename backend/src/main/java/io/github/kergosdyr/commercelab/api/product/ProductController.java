package io.github.kergosdyr.commercelab.api.product;

import java.util.List;

import io.github.kergosdyr.commercelab.api.product.response.ProductResponse;
import io.github.kergosdyr.commercelab.domain.product.ProductListQuery;
import io.github.kergosdyr.commercelab.domain.product.ProductService;
import io.github.kergosdyr.commercelab.support.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> listProducts() {
        var products = productService.listProducts(new ProductListQuery()).stream()
                .map(ProductResponse::fromEntity)
                .toList();
        return ApiResponse.success(products);
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(ProductResponse.fromEntity(productService.getProduct(productId)));
    }
}
