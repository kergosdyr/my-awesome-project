import 'server-only'

import fs from 'node:fs'
import path from 'node:path'
import matter from 'gray-matter'

export interface BlogPostMeta {
  category: string
  coverAlt: string
  coverImage: string
  date: string
  readingTime: string
  series?: string
  seriesOrder?: number
  slug: string
  summary: string
  title: string
}

export interface BlogPost extends BlogPostMeta {
  content: string
}

const postsDirectory = path.join(process.cwd(), 'src/content/posts')

function readPost(fileName: string, directory = postsDirectory): BlogPost {
  const source = fs.readFileSync(path.join(directory, fileName), 'utf8')
  const { data, content } = matter(source)
  const series = typeof data.series === 'string' && data.series.trim()
    ? data.series.trim()
    : undefined
  const parsedSeriesOrder = Number(data.seriesOrder)
  const seriesOrder = series && Number.isInteger(parsedSeriesOrder) && parsedSeriesOrder > 0
    ? parsedSeriesOrder
    : undefined

  return {
    slug: fileName.replace(/\.md$/, ''),
    category: String(data.category),
    coverAlt: String(data.coverAlt),
    coverImage: String(data.coverImage),
    date: String(data.date),
    readingTime: String(data.readingTime),
    series,
    seriesOrder,
    summary: String(data.summary),
    title: String(data.title),
    content,
  }
}

export function getBlogPosts(): BlogPostMeta[] {
  return fs
    .readdirSync(postsDirectory)
    .filter((fileName) => fileName.endsWith('.md'))
    .map((fileName) => readPost(fileName))
    .toSorted((a, b) => b.date.localeCompare(a.date))
    .map((post) => ({
      slug: post.slug,
      category: post.category,
      coverAlt: post.coverAlt,
      coverImage: post.coverImage,
      date: post.date,
      readingTime: post.readingTime,
      series: post.series,
      seriesOrder: post.seriesOrder,
      summary: post.summary,
      title: post.title,
    }))
}

export function getSeriesPosts(series: string): BlogPostMeta[] {
  return getBlogPosts()
    .filter((post) => post.series === series)
    .toSorted((a, b) => {
      const orderDifference = (a.seriesOrder ?? Number.MAX_SAFE_INTEGER)
        - (b.seriesOrder ?? Number.MAX_SAFE_INTEGER)

      return orderDifference || a.date.localeCompare(b.date) || a.title.localeCompare(b.title)
    })
}

export function getBlogPost(slug: string): BlogPost | undefined {
  const filePath = path.join(postsDirectory, `${slug}.md`)
  if (!fs.existsSync(filePath)) {
    if (process.env.NODE_ENV === 'development' && process.env.BLOG_PREVIEW_DRAFTS === '1'
      && /^[a-z0-9-]+$/.test(slug)) {
      const directory = path.join(process.cwd(), 'drafts')
      if (fs.existsSync(path.join(directory, `${slug}.md`))) return readPost(`${slug}.md`, directory)
    }
    return undefined
  }
  return readPost(`${slug}.md`)
}

export function formatPostDate(date: string) {
  return date.replaceAll('-', '.')
}

// Preview-only routes never enter public post lists or production static params.
export function getDraftPreviewSlugs(): string[] {
  if (process.env.NODE_ENV !== 'development' || process.env.BLOG_PREVIEW_DRAFTS !== '1') return []
  const directory = path.join(process.cwd(), 'drafts')
  if (!fs.existsSync(directory)) return []
  return fs.readdirSync(directory)
    .filter((file) => /^[a-z0-9-]+\.md$/.test(file))
    .map((file) => file.replace(/\.md$/, ''))
}
