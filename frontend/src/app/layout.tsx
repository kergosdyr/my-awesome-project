import type { Metadata, Viewport } from 'next'
import localFont from 'next/font/local'
import Script from 'next/script'
import type { ReactNode } from 'react'
import { AppShell } from '@/components/site/AppShell'
import './globals.css'
import '@/features/storefront/storefront.css'

const wantedSans = localFont({
  src: '../../node_modules/wanted-sans/fonts/webfonts/variable/complete/woff2/WantedSansVariable.woff2',
  variable: '--font-wanted',
  display: 'swap',
  weight: '400 1000',
})

const ibmPlexMono = localFont({
  src: '../../node_modules/@fontsource/ibm-plex-mono/files/ibm-plex-mono-latin-400-normal.woff2',
  variable: '--font-ibm-plex-mono',
  display: 'swap',
  weight: '400',
})

export const metadata: Metadata = {
  title: {
    default: 'Justin — Backend Engineer',
    template: '%s — Justin',
  },
  description:
    '오래 버티는 백엔드를 설계하고, 실험을 글과 작은 제품으로 남기는 개발자 Justin의 개인 사이트',
}

export const viewport: Viewport = {
  colorScheme: 'light dark',
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#eceae4' },
    { media: '(prefers-color-scheme: dark)', color: '#090a08' },
  ],
}

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html
      lang="ko"
      className={`${wantedSans.variable} ${ibmPlexMono.variable}`}
      suppressHydrationWarning
      data-scroll-behavior="smooth"
    >
      <body>
        <Script id="theme-init" strategy="beforeInteractive">
          {`try{const t=localStorage.getItem('theme');const d=t==='dark'||(!t&&matchMedia('(prefers-color-scheme: dark)').matches);document.documentElement.dataset.theme=d?'dark':'light';document.documentElement.style.colorScheme=d?'dark':'light'}catch{}`}
        </Script>
        <AppShell>{children}</AppShell>
      </body>
    </html>
  )
}
