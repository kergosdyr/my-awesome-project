import type { Metadata } from 'next'
import Link from 'next/link'
import { notFound } from 'next/navigation'
import { ArrowLeft } from '@phosphor-icons/react/dist/ssr'
import { blogPosts } from '@/content/blog-posts'

interface BlogPostPageProps {
  params: Promise<{ slug: string }>
}

export function generateStaticParams() {
  return blogPosts.map(({ slug }) => ({ slug }))
}

export async function generateMetadata({ params }: BlogPostPageProps): Promise<Metadata> {
  const { slug } = await params
  const post = blogPosts.find((candidate) => candidate.slug === slug)
  return post ? { title: post.title, description: post.summary } : {}
}

export default async function BlogPostPage({ params }: BlogPostPageProps) {
  const { slug } = await params
  const post = blogPosts.find((candidate) => candidate.slug === slug)
  if (!post) notFound()

  return (
    <main className="article-page">
      <Link className="article-back" href="/blog">
        <ArrowLeft aria-hidden="true" /> Blog
      </Link>
      <article>
        <header>
          <span className="micro-label">{post.category} · {post.date} · {post.readingTime}</span>
          <h1>{post.title}</h1>
          <p>{post.summary}</p>
        </header>
        <div className="article-body">
          {post.paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
        </div>
        <footer>
          <span>[ END OF NOTE ]</span>
          <Link href="/blog">다른 글 읽기</Link>
        </footer>
      </article>
    </main>
  )
}
