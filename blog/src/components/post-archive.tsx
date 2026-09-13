'use client'

import { useState } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { ArrowRight, MagnifyingGlass } from '@phosphor-icons/react'
import type { BlogPostMeta } from '@/content/posts'

export function PostArchive({ posts }: { posts: BlogPostMeta[] }) {
  const [category, setCategory] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const categories = [...new Set(posts.map((post) => post.category))]
  const search = query.trim().toLocaleLowerCase('ko-KR')
  const visiblePosts = posts.filter((post) => (
    (category === null || post.category === category)
    && `${post.title} ${post.summary} ${post.category}`.toLocaleLowerCase('ko-KR').includes(search)
  ))

  return (
    <section className="archive" id="archive" aria-label="글 목록">
      <div className="archive-toolbar">
        <div className="archive-filters" role="group" aria-label="글 주제">
          <button type="button" aria-pressed={category === null} onClick={() => setCategory(null)}>전체</button>
          {categories.map((item) => (
            <button type="button" key={item} aria-pressed={category === item} onClick={() => setCategory(item)}>{item}</button>
          ))}
        </div>
        <label className="archive-search">
          <MagnifyingGlass size={17} aria-hidden="true" />
          <span className="visually-hidden">글 검색</span>
          <input type="search" placeholder="글 검색…" value={query} onChange={(event) => setQuery(event.target.value)} />
        </label>
      </div>
      <p className="visually-hidden" role="status">{visiblePosts.length}개의 글</p>
      {visiblePosts.length > 0 ? (
        <div className="post-grid">
          {visiblePosts.map((post, index) => (
            <article className="post-card" key={post.slug}>
              <Link href={`/${post.slug}`} aria-labelledby={`post-${post.slug}`}>
                <div className="post-card__visual">
                  <Image src={post.coverImage} alt={post.coverAlt} fill priority={index < 3}
                    sizes="(max-width: 640px) calc(100vw - 40px), (max-width: 1100px) calc((100vw - 96px) / 2), (max-width: 1408px) calc((100vw - 192px) / 3), 384px" />
                </div>
                <div className="post-card__body">
                  <h2 id={`post-${post.slug}`}>{post.title}</h2>
                  <p className="post-card__summary">{post.summary}</p>
                  <div className="post-card__meta">
                    <span>{post.category}<span className="post-card__dot" aria-hidden="true">·</span><time dateTime={post.date}>{post.date.replaceAll('-', '.')}</time></span>
                    <ArrowRight size={16} aria-hidden="true" />
                  </div>
                </div>
              </Link>
            </article>
          ))}
        </div>
      ) : (
        <div className="archive-empty">
          <h2>{posts.length === 0 ? '새로운 글을 준비하고 있습니다.' : '검색 결과가 없습니다.'}</h2>
          <p>{posts.length === 0 ? '질문하고, 실험하고, 배운 것들을 이곳에 기록합니다.' : '다른 검색어나 주제로 찾아보세요.'}</p>
          {posts.length > 0 && <button type="button" onClick={() => { setQuery(''); setCategory(null) }}>전체 글 보기 <ArrowRight size={16} aria-hidden="true" /></button>}
        </div>
      )}
    </section>
  )
}
