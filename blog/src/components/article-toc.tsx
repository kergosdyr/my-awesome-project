'use client'

import { useEffect, useState } from 'react'
import type { ArticleHeading } from '@/lib/article-outline'

type TocHeading = Omit<ArticleHeading, 'line'>

interface ArticleTocProps {
  headings: TocHeading[]
}

function TocItems({ activeId, headings }: ArticleTocProps & { activeId: string }) {
  return (
    <ol>
      {headings.map((heading) => {
        const isActive = activeId === heading.id

        return (
          <li className={isActive ? 'is-active' : undefined} data-depth={heading.depth} key={heading.id}>
            <a href={`#${heading.id}`} aria-current={isActive ? 'location' : undefined}>
              <span>{heading.title}</span>
              <span className="article-toc__arrow" aria-hidden="true">›</span>
            </a>
          </li>
        )
      })}
    </ol>
  )
}

export function ArticleToc({ headings }: ArticleTocProps) {
  const [activeId, setActiveId] = useState(headings[0]?.id ?? '')

  useEffect(() => {
    const sections = headings
      .map(({ id }) => document.getElementById(id))
      .filter((section): section is HTMLElement => section !== null)

    if (sections.length === 0) return

    let frame: number | undefined

    const updateActiveHeading = () => {
      frame = undefined
      const readingLine = 150
      let current = sections[0]

      if (window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 2) {
        setActiveId(sections.at(-1)?.id ?? current.id)
        return
      }

      for (const section of sections) {
        if (section.getBoundingClientRect().top > readingLine) break
        current = section
      }

      setActiveId(current.id)
    }

    const handleScroll = () => {
      if (frame !== undefined) return
      frame = window.requestAnimationFrame(updateActiveHeading)
    }

    updateActiveHeading()
    window.addEventListener('scroll', handleScroll, { passive: true })

    return () => {
      window.removeEventListener('scroll', handleScroll)
      if (frame !== undefined) window.cancelAnimationFrame(frame)
    }
  }, [headings])

  if (headings.length === 0) return null

  return (
    <div className="article-toc">
      <nav className="article-toc__desktop" aria-label="이 글의 목차">
        <p className="article-toc__label">Contents</p>
        <TocItems activeId={activeId} headings={headings} />
      </nav>

      <details className="article-toc__mobile">
        <summary>
          <span>이 글의 목차</span>
          <span className="article-toc__toggle" aria-hidden="true" />
        </summary>
        <nav aria-label="이 글의 목차">
          <TocItems activeId={activeId} headings={headings} />
        </nav>
      </details>
    </div>
  )
}
