package challenge.commerce.api;

import challenge.commerce.api.response.ProductResponse;
import challenge.commerce.domain.catalog.*;
import challenge.commerce.domain.query.PageQuery;
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
    public List<ProductResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return catalogService.list(new PageQuery(page, size)).stream()
                .map(ProductResponse::from)
                .toList();
    }
}
