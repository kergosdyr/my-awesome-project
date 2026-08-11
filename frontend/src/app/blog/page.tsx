import type { Metadata } from 'next'
import { ArrowUpRight } from '@phosphor-icons/react/dist/ssr'
import Link from 'next/link'
import { blogPosts } from '@/content/blog-posts'

export const metadata: Metadata = {
  title: 'Blog',
  description: '백엔드 설계, 성능 실험, 제품을 만들며 배운 것을 기록합니다.',
}

export default function BlogPage() {
  return (
    <main className="page-shell blog-page">
      <header className="page-hero">
        <span className="micro-label">Notes on building systems</span>
        <h1>BLOG</h1>
        <p>설계의 이유, 실패에서 배운 것, 다음 실험을 오래 기억하기 위한 기록.</p>
      </header>

      <section className="blog-index" aria-label="블로그 글 목록">
        {blogPosts.map((post, index) => (
          <Link href={`/blog/${post.slug}`} className="blog-row" key={post.slug}>
            <span className="blog-row__number">0{index + 1}</span>
            <div className="blog-row__title">
              <span className="micro-label">{post.category}</span>
              <h2>{post.title}</h2>
              <p>{post.summary}</p>
            </div>
            <div className="blog-row__meta">
              <span>{post.date}</span>
              <span>{post.readingTime}</span>
            </div>
            <ArrowUpRight aria-hidden="true" />
          </Link>
        ))}
      </section>
    </main>
  )
}
