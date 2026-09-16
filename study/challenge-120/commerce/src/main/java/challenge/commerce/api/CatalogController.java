package challenge.commerce.api;

import challenge.commerce.api.response.ProductResponse;
import challenge.commerce.domain.catalog.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return catalog.list().stream().map(ProductResponse::from).toList();
    }
}
