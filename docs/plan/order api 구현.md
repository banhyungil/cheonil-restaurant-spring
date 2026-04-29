1. 전체 더미 — 실 axios 전환 필요
   \_orders in-memory 배열 위에서 모두 동작. 백엔드 GET API 구현 후 axios 전환:

return api.get<OrderExt[]>('/orders', { params }).then(r => r.data)

2. OrderExt 와 백엔드 DTO 불일치
   프론트는 storeNm + menus[{menuNm, ...}] flat aggregate 기대
   백엔드 현재 OrderRes 는 {items: [{menuSeq, price, cnt}]} 만 (menu nm 없음)
   → 백엔드에 OrderListRes, OrderDetailRes 같은 join DTO 신설 필요 (이전 답변에서 제안한 구조)

3. updateStatus 의 비즈니스 로직이 프론트에

if (status === 'COOKED') order.cookedAt = new Date().toISOString()
else if (status === 'READY') order.cookedAt = null
클라 시계 신뢰 → 단말 시간 어긋나면 정렬 깨짐
타임존 / DST 사고 위험
→ 서버가 처리해야 하는 룰. 실 API 전환 시 클라에선 status 만 보내고 cookedAt 응답으로 받기

4. 배열 query param 직렬화 미정
   statuses?: OrderStatus[] 를 axios 에 그대로 넘기면:

기본: ?statuses[]=READY&statuses[]=COOKED (qs 스타일 — Spring 은 이 형태 default 매개변수 바인딩 안 됨)
Spring 친화: ?statuses=READY&statuses=COOKED (no brackets)
또는: ?statuses=READY,COOKED (콤마 join, 서버에서 split)
→ 결정 + paramsSerializer 명시 필요:

import qs from 'qs'
api.get('/orders', {
params,
paramsSerializer: { serialize: (p) => qs.stringify(p, { arrayFormat: 'repeat' }) }
})
