import type { Metadata, Viewport } from 'next'
import localFont from 'next/font/local'
import type { ReactNode } from 'react'
import { SiteFooter } from '@/components/site-footer'
import { SiteHeader } from '@/components/site-header'
import '@xyflow/react/dist/style.css'
import './globals.css'

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

const newsreader = localFont({
  src: '../../node_modules/@fontsource-variable/newsreader/files/newsreader-latin-wght-italic.woff2',
  variable: '--font-newsreader',
  display: 'swap',
  style: 'italic',
  weight: '200 800',
})

const chosunNm = localFont({
  src: '../fonts/ChosunNm.woff2',
  variable: '--font-chosun-nm',
  display: 'swap',
  weight: '400',
})

export const metadata: Metadata = {
  title: {
    default: 'Justin Blog',
    template: '%s — Justin Blog',
  },
  icons: { icon: { url: '/brand/justin-blog-icon.svg', type: 'image/svg+xml' } },
  description: '백엔드 설계, 성능 실험, 제품을 만들며 배운 것을 기록합니다.',
}

export const viewport: Viewport = {
  colorScheme: 'dark',
  themeColor: '#090909',
}

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html
      lang="ko"
      className={`${wantedSans.variable} ${ibmPlexMono.variable} ${newsreader.variable} ${chosunNm.variable}`}
      data-scroll-behavior="smooth"
    >
      <body>
        <div className="site-stage">
          <div className="site-frame">
            <SiteHeader />
            {children}
            <SiteFooter />
          </div>
        </div>
      </body>
    </html>
  )
}
