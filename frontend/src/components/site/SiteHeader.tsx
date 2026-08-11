'use client'

import { ArrowUpRight, Moon, Sun } from '@phosphor-icons/react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'

const navigation = [
  { href: '/', label: 'Home' },
  { href: '/#about', label: 'About' },
  { href: '/blog', label: 'Blog' },
]

export function ThemeToggle() {
  const toggleTheme = () => {
    const nextDark = document.documentElement.dataset.theme !== 'dark'
    document.documentElement.dataset.theme = nextDark ? 'dark' : 'light'
    document.documentElement.style.colorScheme = nextDark ? 'dark' : 'light'
    localStorage.setItem('theme', nextDark ? 'dark' : 'light')
  }

  return (
    <button
      className="theme-toggle"
      type="button"
      onClick={toggleTheme}
      aria-label="색상 모드 전환"
      title="색상 모드 전환"
    >
      <Moon className="theme-icon theme-icon--moon" weight="regular" aria-hidden="true" />
      <Sun className="theme-icon theme-icon--sun" weight="regular" aria-hidden="true" />
    </button>
  )
}

export function SiteHeader() {
  const pathname = usePathname()

  return (
    <header className="site-header">
      <Link className="site-brand" href="/" aria-label="Justin 홈으로 이동">
        <span className="site-brand__mark" aria-hidden="true">J</span>
        <span>Justin</span>
      </Link>

      <nav className="site-nav" aria-label="주요 메뉴">
        {navigation.map((item) => {
          const active =
            item.href === '/'
              ? pathname === '/'
              : item.href.startsWith('/#')
                ? false
                : pathname.startsWith(item.href)

          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? 'page' : undefined}
            >
              {item.label}
              <span aria-hidden="true">+</span>
            </Link>
          )
        })}
        <Link href="/shop" target="_blank" rel="noreferrer">
          Demo Shop
          <ArrowUpRight aria-hidden="true" />
        </Link>
      </nav>

      <div className="site-header__meta">
        <span>Local time</span>
        <strong>SEOUL · GMT+9</strong>
      </div>
      <ThemeToggle />
    </header>
  )
}
