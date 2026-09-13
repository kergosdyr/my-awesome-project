import Link from 'next/link'

export default function NotFound() {
  return (
    <main className="not-found" id="main-content">
      <span className="eyebrow">404 · Note not found</span>
      <h1>글을 찾을 수 없습니다.</h1>
      <Link href="/">블로그로 돌아가기</Link>
    </main>
  )
}
