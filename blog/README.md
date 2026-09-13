# Justin Blog

커머스/백엔드 실험 앱과 분리된 독립 Next.js 블로그입니다. 글은 Markdown으로 관리하고, 글마다 커버 이미지 한 장을 지정할 수 있습니다.

## 로컬 실행

```bash
npm install
npm run dev
```

기본 주소는 `http://localhost:3000`입니다.

## 글 작성

글과 이미지를 추가하는 방법은 [WRITING.md](./WRITING.md)를 참고합니다.

## Vercel 배포

Git 저장소를 Vercel에 연결한 뒤 프로젝트의 **Root Directory**를 `blog`로 지정합니다. Framework Preset은 Next.js로 자동 감지되며 별도의 빌드 설정은 필요하지 않습니다.

사이트맵과 canonical URL을 위해 Vercel 환경 변수에 실제 주소를 등록합니다.

```text
NEXT_PUBLIC_SITE_URL=https://blog.example.com
```

CLI로 preview 배포할 때는 저장소 루트에서 아래 명령을 사용할 수 있습니다.

```bash
npx vercel --cwd blog
```
