'use client'

import { usePathname } from 'next/navigation'
import type { ReactNode } from 'react'
import { SiteFooter } from './SiteFooter'
import { SiteHeader } from './SiteHeader'

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname()

  if (pathname.startsWith('/shop')) return children

  return (
    <div className="site-stage">
      <div className="site-frame">
        <SiteHeader />
        {children}
        <SiteFooter />
      </div>
    </div>
  )
}
