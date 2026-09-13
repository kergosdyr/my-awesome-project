import type { MetadataRoute } from 'next'
import { getBlogPosts } from '@/content/posts'

export default function sitemap(): MetadataRoute.Sitemap {
  const siteUrl = (process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000').replace(/\/$/, '')
  const posts = getBlogPosts()

  return [
    { url: siteUrl, changeFrequency: 'monthly', priority: 1 },
    ...posts.map((post) => ({
      url: `${siteUrl}/${post.slug}`,
      lastModified: new Date(post.date.replaceAll('.', '-')),
      changeFrequency: 'yearly' as const,
      priority: 0.7,
    })),
  ]
}
