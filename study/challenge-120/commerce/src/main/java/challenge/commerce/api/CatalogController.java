package challenge.commerce.api;

import challenge.commerce.api.response.ProductResponse;
import challenge.commerce.domain.catalog.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        return catalogService.list().stream().map(ProductResponse::from).toList();
    }
}
