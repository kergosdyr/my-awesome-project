export interface ArticleHeading {
  depth: 2 | 3
  id: string
  line: number
  title: string
}

function plainHeadingText(value: string) {
  return value
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/<[^>]+>/g, '')
    .replace(/[`*_~]/g, '')
    .replace(/\\([\\`*_[\]{}()#+.!-])/g, '$1')
    .trim()
}

function headingSlug(title: string) {
  return title
    .normalize('NFKC')
    .toLocaleLowerCase('ko-KR')
    .replace(/[^\p{Letter}\p{Number}\s-]/gu, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
}

export function getArticleOutline(markdown: string): ArticleHeading[] {
  const slugCounts = new Map<string, number>()
  const headings: ArticleHeading[] = []
  let fence: '`' | '~' | undefined

  markdown.split('\n').forEach((line, index) => {
    const fenceMatch = line.match(/^\s*(`{3,}|~{3,})/)

    if (fenceMatch) {
      const nextFence = fenceMatch[1][0] as '`' | '~'
      fence = fence === nextFence ? undefined : nextFence
      return
    }

    if (fence) return

    const match = line.match(/^(#{2,3})\s+(.+?)\s*#*\s*$/)
    if (!match) return

    const title = plainHeadingText(match[2])
    const baseSlug = headingSlug(title) || 'section'
    const duplicateCount = slugCounts.get(baseSlug) ?? 0
    slugCounts.set(baseSlug, duplicateCount + 1)

    headings.push({
      depth: match[1].length as 2 | 3,
      id: duplicateCount === 0 ? baseSlug : `${baseSlug}-${duplicateCount + 1}`,
      line: index + 1,
      title,
    })
  })

  return headings
}
