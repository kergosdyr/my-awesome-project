import Link from 'next/link'

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <p>Systems, failures,<br />and everything between.</p>
      <Link href="/">Justin Blog</Link>
      <div>
        <span>SEOUL · GMT+9</span>
        <span>© {new Date().getFullYear()}</span>
      </div>
    </footer>
  )
}
