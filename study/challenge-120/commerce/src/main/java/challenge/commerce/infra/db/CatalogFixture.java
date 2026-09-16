package challenge.commerce.infra.db;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogFixture implements CommandLineRunner {
    private final ProductJpaRepository products;
    private final ProductOptionJpaRepository options;

    public CatalogFixture(ProductJpaRepository products, ProductOptionJpaRepository options) {
        this.products = products;
        this.options = options;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (products.count() > 0) return;
        products.saveAll(List.of(
                new ProductEntity(
                        1,
                        "ORDINARY STUDIO",
                        "유틸리티 필드 재킷",
                        "가볍게 걸치는 구조적인 실루엣. 매트한 코튼과 넉넉한 포켓, 매일의 움직임을 위한 재킷.",
                        "OUTER",
                        129000,
                        "/images/jacket.svg"),
                new ProductEntity(
                        2,
                        "FORM ESSENTIALS",
                        "헤비웨이트 코튼 티셔츠",
                        "탄탄한 20수 코튼과 편안한 어깨선. 계절을 넘어 옷장에 남을 기본.",
                        "TOP",
                        39000,
                        "/images/tee.svg"),
                new ProductEntity(
                        3,
                        "STILL OBJECTS",
                        "데일리 캔버스 토트",
                        "일상의 물건을 담는 단정한 형태. 두꺼운 캔버스와 넓은 수납 공간.",
                        "BAG",
                        59000,
                        "/images/tote.svg")));
        options.saveAll(List.of(
                new ProductOptionEntity(101, 1, "Olive", "M", 12),
                new ProductOptionEntity(102, 1, "Olive", "L", 8),
                new ProductOptionEntity(103, 1, "Olive", "XL", 0),
                new ProductOptionEntity(201, 2, "Chalk", "S", 20),
                new ProductOptionEntity(202, 2, "Chalk", "M", 20),
                new ProductOptionEntity(203, 2, "Chalk", "L", 14),
                new ProductOptionEntity(301, 3, "Natural", "ONE SIZE", 9)));
    }
}
