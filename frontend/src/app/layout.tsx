import type { Metadata, Viewport } from 'next'
import type { ReactNode } from 'react'
import './globals.css'
import '@/features/storefront/storefront.css'

export const metadata: Metadata = {
  title: '커머스 실험실',
  description: '프런트엔드와 백엔드 기술을 안전하게 실험하는 작은 커머스 콘솔',
}

export const viewport: Viewport = {
  themeColor: '#ffffff',
}

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="ko" data-scroll-behavior="smooth">
      <body>{children}</body>
    </html>
  )
}
