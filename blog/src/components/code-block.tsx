'use client'

import { Check, Copy, CornersIn, CornersOut } from '@phosphor-icons/react'
import { useEffect, useRef, useState } from 'react'

const LANGUAGE_LABELS: Record<string, string> = {
  c: 'C',
  java: 'Java',
  lua: 'Lua',
  plaintext: 'Text',
  python: 'Python',
  redis: 'Redis CLI',
  sql: 'SQL',
  text: 'Text',
}

const LANGUAGE_MARKS: Record<string, string> = {
  c: 'C',
  java: 'JV',
  lua: 'LU',
  plaintext: '>_',
  python: 'PY',
  redis: 'R',
  sql: 'SQ',
  text: '>_',
}

interface CodeBlockProps {
  code: string
  highlightedCode: string
  language: string
  lineCount: number
}

function copyWithFallback(value: string) {
  const textarea = document.createElement('textarea')
  textarea.value = value
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.append(textarea)
  textarea.select()
  document.execCommand('copy')
  textarea.remove()
}

export function CodeBlock({ code, highlightedCode, language, lineCount }: CodeBlockProps) {
  const [copied, setCopied] = useState(false)
  const [expanded, setExpanded] = useState(false)
  const copiedTimer = useRef<ReturnType<typeof setTimeout>>(undefined)
  const expandable = lineCount > 14
  const label = LANGUAGE_LABELS[language] ?? language.toUpperCase()
  const mark = LANGUAGE_MARKS[language] ?? language.slice(0, 2).toUpperCase()

  useEffect(() => () => clearTimeout(copiedTimer.current), [])

  async function handleCopy() {
    try {
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(code)
      } else {
        copyWithFallback(code)
      }
      setCopied(true)
      clearTimeout(copiedTimer.current)
      copiedTimer.current = setTimeout(() => setCopied(false), 1800)
    } catch {
      copyWithFallback(code)
      setCopied(true)
    }
  }

  return (
    <figure className={`code-frame${expandable && !expanded ? ' is-collapsed' : ''}`}>
      <figcaption className="code-frame__bar">
        <span className="code-frame__identity">
          <span className="code-frame__mark" aria-hidden="true">{mark}</span>
          <span>{label}</span>
        </span>
        <button
          className="code-frame__action"
          type="button"
          onClick={handleCopy}
          aria-label={copied ? '코드가 복사되었습니다' : '코드 복사'}
        >
          {copied ? <Check aria-hidden="true" /> : <Copy aria-hidden="true" />}
          <span aria-hidden="true">{copied ? '복사됨' : '복사'}</span>
        </button>
      </figcaption>

      <div
        className="code-frame__viewport"
        dangerouslySetInnerHTML={{ __html: highlightedCode }}
      />

      {expandable && (
        <div className="code-frame__expand">
          <button
            type="button"
            onClick={() => setExpanded((value) => !value)}
            aria-expanded={expanded}
          >
            {expanded ? <CornersIn aria-hidden="true" /> : <CornersOut aria-hidden="true" />}
            {expanded ? '코드 접기' : `전체 ${lineCount}줄 펼치기`}
          </button>
        </div>
      )}
    </figure>
  )
}
