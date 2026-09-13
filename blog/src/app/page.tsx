import type { Metadata } from 'next'
import { PostArchive } from '@/components/post-archive'
import { getBlogPosts } from '@/content/posts'

export const metadata: Metadata = {
  title: { absolute: 'Justin Blog' },
  description: '백엔드 설계, 성능 실험, 제품을 만들며 배운 것을 기록합니다.',
}

export default function BlogPage() {
  return (
    <main className="journal-page" id="main-content">
      <header className="journal-heading"><h1>Blog</h1></header>
      <PostArchive posts={getBlogPosts()} />
    </main>
  )
}
