'use client'

import { useEffect, useId, useState } from 'react'

interface MermaidDiagramProps {
  source: string
}

let mermaidInitialized = false
let renderQueue = Promise.resolve()

async function renderMermaid(id: string, source: string) {
  const mermaid = (await import('mermaid')).default

  if (!mermaidInitialized) {
    mermaid.initialize({
      startOnLoad: false,
      securityLevel: 'strict',
      theme: 'dark',
      themeVariables: {
        background: '#151519',
        primaryColor: '#242129',
        primaryTextColor: '#f7f9fa',
        primaryBorderColor: '#af50ff',
        lineColor: '#85858e',
        secondaryColor: '#17171c',
        tertiaryColor: '#1b1b20',
        fontFamily: 'var(--font-sans)',
      },
    })
    mermaidInitialized = true
  }

  const { svg } = await mermaid.render(id, source)
  return svg
}

export function MermaidDiagram({ source }: MermaidDiagramProps) {
  const reactId = useId()
  const [svg, setSvg] = useState<string>()
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    const diagramId = `mermaid-${reactId.replaceAll(':', '')}`

    const render = async () => {
      try {
        const nextSvg = await renderMermaid(diagramId, source)
        if (active) setSvg(nextSvg)
      } catch {
        if (active) setFailed(true)
      }
    }

    renderQueue = renderQueue.then(render, render)

    return () => {
      active = false
    }
  }, [reactId, source])

  if (failed) {
    return (
      <figure className="mermaid-diagram mermaid-diagram--fallback">
        <figcaption>다이어그램을 표시하지 못해 원본을 보여드립니다.</figcaption>
        <pre><code>{source}</code></pre>
      </figure>
    )
  }

  return (
    <figure
      className="mermaid-diagram"
      aria-busy={!svg}
      aria-label="본문 다이어그램"
    >
      {svg
        ? <div className="mermaid-diagram__canvas" dangerouslySetInnerHTML={{ __html: svg }} />
        : <span className="mermaid-diagram__loading">다이어그램을 그리는 중…</span>}
    </figure>
  )
}
