import Link from 'next/link'

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <p>Designed with curiosity. Engineered for change.</p>
      <nav aria-label="푸터 메뉴">
        <Link href="/blog">Blog</Link>
        <Link href="/shop">Shop</Link>
        <a href="https://github.com/kergosdyr" target="_blank" rel="noreferrer">
          GitHub
        </a>
      </nav>
      <span>© {new Date().getFullYear()} JUSTIN</span>
    </footer>
  )
}
