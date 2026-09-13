import Link from 'next/link'
import Image from 'next/image'

export function SiteHeader() {
  return (
    <header className="site-header">
      <a className="skip-link" href="#main-content">본문으로 건너뛰기</a>
      <Link className="site-brand" href="/" aria-label="Justin Blog 홈으로 이동">
        <Image className="site-brand__mark" src="/brand/justin-blog-mark.svg" alt="" width={26} height={26} />
        <span>Justin Blog</span>
      </Link>

      <nav className="site-nav" aria-label="주요 메뉴">
        <Link href="/#archive">글 목록</Link>
        <a href="https://github.com/kergosdyr" target="_blank" rel="noreferrer">GitHub</a>
      </nav>
    </header>
  )
}
