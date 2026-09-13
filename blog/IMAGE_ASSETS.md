# Sample cover assets

현재 네 장의 커버 이미지는 레이아웃 확인용 샘플이며 `public/images/posts/`에 저장되어 있습니다.

- `architecture-boundaries.png`
- `measure-performance.png`
- `frontend-observability.png`
- `commerce-concurrency.png`

Built-in Image Gen을 사용했습니다. 공통 프롬프트는 다음 방향으로 제작했습니다.

> 4:3 editorial technology blog cover. Premium soft 3D object on a near-black graphite studio background, with restrained signal-violet lighting, off-white and steel accents, generous negative space, and no text, logos, people, finance symbols, or watermark.

각 커버의 주제는 모듈 경계, 성능 측정 장치, 프런트엔드 관측 장치, 커머스 동시성입니다. 실제 글을 발행할 때는 같은 경로의 파일을 교체하거나 Markdown의 `coverImage` 값을 새 파일 경로로 변경하면 됩니다.

## 예약과 취소의 락 커버 — 2026-09-10

- 파일: `public/images/posts/reservation-locks-and-transaction-boundaries.png`
- 생성: Built-in Image Gen
- 프롬프트:

> Use case: stylized-concept. Asset type: 4:3 landscape editorial thumbnail for a Korean backend engineering blog article about shared and exclusive database locks, reservations and cancellations. Create a polished tactile 3D editorial illustration: small reservation-ticket cards moving along two orderly lanes toward a single shared database record, represented by a solid stacked cylinder and one prominent physical lock. One lane shows two cool-blue translucent access tokens side by side (shared access), the other one warm amber token at a closed gate with other tokens waiting in a neat queue (exclusive access). Clear simple composition that reads at small thumbnail size, generous margins safe for cropping, sophisticated dark navy and warm ivory with restrained amber and blue accents, softly lit matte ceramic and brushed metal materials, subtle shadows, no clutter. Conceptual illustration, not a technical diagram; no arrows implying that every read is blocked. No text, letters, numbers, logos or watermarks. Output one high-quality 4:3 image.
