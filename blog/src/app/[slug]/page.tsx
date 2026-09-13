import type { Metadata } from 'next'
import { isValidElement, type ComponentPropsWithoutRef, type ReactNode } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { notFound } from 'next/navigation'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { codeToHtml, type BundledLanguage, type SpecialLanguage } from 'shiki'
import { CodeBlock } from '@/components/code-block'
import { FactorGrid, ProcessFlow } from '@/components/concept-diagram'
import { ArticleChart } from '@/components/article-chart'
import { MermaidDiagram } from '@/components/mermaid-diagram'
import { ArticleToc } from '@/components/article-toc'
import { formatPostDate, getBlogPost, getBlogPosts, getSeriesPosts, getDraftPreviewSlugs } from '@/content/posts'
import { getArticleOutline } from '@/lib/article-outline'

const LANGUAGE_ALIASES: Record<string, BundledLanguage | SpecialLanguage> = {
  redis: 'shellscript',
  text: 'plaintext',
}

async function MarkdownPre({ children, ...props }: ComponentPropsWithoutRef<'pre'>) {
  if (isValidElement<{ className?: string; children?: ReactNode }>(children)
    && children.props.className?.split(' ').includes('language-chart')) {
    return <ArticleChart source={String(children.props.children).replace(/\n$/, '')} />
  }

  if (
    isValidElement<{ className?: string; children?: ReactNode }>(children)
    && children.props.className?.split(' ').includes('language-mermaid')
  ) {
    return <MermaidDiagram source={String(children.props.children).replace(/\n$/, '')} />
  }

  if (
    isValidElement<{ className?: string; children?: ReactNode }>(children)
    && children.props.className?.split(' ').includes('language-flow')
  ) {
    return <ProcessFlow source={String(children.props.children).replace(/\n$/, '')} />
  }

  if (
    isValidElement<{ className?: string; children?: ReactNode }>(children)
    && children.props.className?.split(' ').includes('language-factors')
  ) {
    return <FactorGrid source={String(children.props.children).replace(/\n$/, '')} />
  }

  if (isValidElement<{ className?: string; children?: ReactNode }>(children)) {
    const language = children.props.className?.match(/language-([^\s]+)/)?.[1] ?? 'text'
    const shikiLanguage = LANGUAGE_ALIASES[language] ?? language as BundledLanguage
    const code = String(children.props.children).replace(/\n$/, '')
    let highlightedCode: string

    try {
      highlightedCode = await codeToHtml(code, {
        lang: shikiLanguage,
        theme: 'github-dark-default',
      })
    } catch {
      highlightedCode = await codeToHtml(code, {
        lang: 'plaintext',
        theme: 'github-dark-default',
      })
    }

    return (
      <CodeBlock
        code={code}
        highlightedCode={highlightedCode}
        language={language}
        lineCount={code.split('\n').length}
      />
    )
  }

  return <pre {...props}>{children}</pre>
}

export function generateStaticParams() {
  return [...getBlogPosts().map(({ slug }) => ({ slug })), ...getDraftPreviewSlugs().map((slug) => ({ slug }))]
}

export const dynamicParams = false

export async function generateMetadata({ params }: PageProps<'/[slug]'>): Promise<Metadata> {
  const { slug } = await params
  const post = getBlogPost(slug)
  return post ? { title: post.title, description: post.summary, ...(getDraftPreviewSlugs().includes(slug) ? { robots: { index: false, follow: false } } : {}) } : {}
}

export default async function BlogPostPage({ params }: PageProps<'/[slug]'>) {
  const { slug } = await params
  const post = getBlogPost(slug)
  if (!post) notFound()
  const seriesPosts = post.series ? getSeriesPosts(post.series) : []
  const articleOutline = getArticleOutline(post.content)
  const headingIdByLine = new Map(articleOutline.map((heading) => [heading.line, heading.id]))
  const tocHeadings = articleOutline.map(({ depth, id, title }) => ({ depth, id, title }))

  return (
    <main className="article-page" id="main-content">
      <article>
        <header className="article-hero">
          <nav className="article-hero__nav" aria-label="현재 위치">
            <Link href="/#archive">Blog</Link><span aria-hidden="true">/</span><span>{post.category}</span>
          </nav>
          <div className="article-hero__copy">
            <h1>{post.title}</h1>
            <p>{post.summary}</p>
          </div>
          <div className="article-cover">
            <Image src={post.coverImage} alt={post.coverAlt} fill priority
              sizes="(max-width: 760px) calc(100vw - 40px), (max-width: 1024px) calc(100vw - 64px), 960px" />
          </div>
          <p className="article-meta"><span>Justin</span><span aria-hidden="true">·</span><time dateTime={post.date}>{formatPostDate(post.date)}</time><span aria-hidden="true">·</span><span>{post.readingTime}</span></p>
        </header>

        <div className="article-layout">
          <div className="article-body">
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                h2: ({ node, ...props }) => (
                  <h2 id={headingIdByLine.get(node?.position?.start.line ?? -1)} {...props} />
                ),
                h3: ({ node, ...props }) => (
                  <h3 id={headingIdByLine.get(node?.position?.start.line ?? -1)} {...props} />
                ),
                table: ({ children }) => (
                  <div className="article-table" role="region" aria-label="본문 표" tabIndex={0}>
                    <table>{children}</table>
                  </div>
                ),
                pre: MarkdownPre,
              }}
            >
              {post.content}
            </ReactMarkdown>
          </div>
          {articleOutline.length > 0 && (
            <aside className="article-aside">
              <ArticleToc headings={tocHeadings} />
            </aside>
          )}
        </div>

        {post.series && seriesPosts.length > 0 && (
          <section className="article-series" aria-labelledby="article-series-title">
            <div className="article-series__heading">
              <p>Series</p>
              <h2 id="article-series-title">{post.series}</h2>
            </div>
            <ol>
              {seriesPosts.map((seriesPost, index) => {
                const isCurrent = seriesPost.slug === post.slug

                return (
                  <li className={isCurrent ? 'is-current' : undefined} key={seriesPost.slug}>
                    <Link href={`/${seriesPost.slug}`} aria-current={isCurrent ? 'page' : undefined}>
                      <span>{String(seriesPost.seriesOrder ?? index + 1).padStart(2, '0')}</span>
                      <strong>{seriesPost.title}</strong>
                      {isCurrent && <small>현재 글</small>}
                    </Link>
                  </li>
                )
              })}
            </ol>
          </section>
        )}
      </article>
    </main>
  )
}
