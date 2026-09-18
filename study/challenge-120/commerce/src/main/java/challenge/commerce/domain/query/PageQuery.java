package challenge.commerce.domain.query;

import challenge.commerce.support.BusinessException;

/** 기본 제공 목록: 0부터 시작하는 페이지, 기본 20개·최대 100개. */
public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw BusinessException.invalid("페이지는 0 이상, 크기는 1~100이며 조회 위치는 정수 범위여야 합니다.");
        }
    }
}
