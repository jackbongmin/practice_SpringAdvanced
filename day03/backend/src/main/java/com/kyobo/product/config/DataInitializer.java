package com.kyobo.product.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.kyobo.product.entity.Product;
import com.kyobo.product.repository.ProductRepository;

/**
 * 3일차 · 03 (쿼리 메서드 · 정렬 · 페이지 · JPQL)
 *
 * [역할] 서버가 뜰 때 상품 8개를 DB에 넣어 둠. 4-5에서 브라우저로 검색 API를 열면 이 8개가 나옴
 *
 * [알아야 할 것]
 * - CommandLineRunner: 스프링이 빈을 다 만든 뒤 run(...)을 한 번 불러 줌. @Component가 있어야 빈이 되어 불림
 * - 데이터는 ProductQueryTest.setUp()과 똑같은 8개 (펜 1000 · 1500 · 2000, 볼펜, 형광펜, 공책, 연필, 필통)
 * - H2 메모리 DB라 서버를 다시 켜면 처음 8개로 돌아감. 번호(number)는 넣은 순서대로 1~8
 * - @DataJpaTest · @WebMvcTest는 @Component를 띄우지 않음 → 테스트에는 이 8개가 들어가지 않음 (ProductRepositoryTest의 빈 DB 테스트도 그대로 통과)
 */
@Component  // 빈으로 등록 -> 스프링이 서버 시작 때 run(. . .)을 불러줌. 빠뜨리면 아무일도 안일어남(오류도 없음)
public class DataInitializer implements CommandLineRunner{  // CommandLineRunner: 빈이 다 만들어진 뒤 run(...)이 한번 불림

    private final ProductRepository products;

    public DataInitializer(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(String... args) {
        products.saveAll(List.of( // 8개를 한 번에 저장. 번호는 넣은 순서대로 1~8
                new Product("펜", 1000, 10),
                new Product("펜", 1500, 5),
                new Product("펜", 2000, 0),
                new Product("볼펜", 800, 30),
                new Product("형광펜", 1200, 12),
                new Product("공책", 3000, 7),
                new Product("연필", 500, 50),
                new Product("필통", 5000, 2)));
    }

}
