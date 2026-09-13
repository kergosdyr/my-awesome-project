'use client'

import {
  Background,
  BackgroundVariant,
  Controls,
  Handle,
  MarkerType,
  Position,
  ReactFlow,
  type Edge,
  type Node,
  type NodeProps,
} from '@xyflow/react'
import { Fragment, memo, type ReactNode, useEffect, useMemo, useState } from 'react'

interface DiagramItem {
  detail?: string
  title: string
}

interface ProcessNodeData extends Record<string, unknown> {
  detail?: string
  hasSource: boolean
  hasTarget: boolean
  index: string
  sourcePosition: Position
  targetPosition: Position
  title: string
}

type ProcessNode = Node<ProcessNodeData, 'process'>

interface Point {
  x: number
  y: number
}

function parseDiagram(source: string, fallbackTitle: string) {
  const lines = source
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)

  const title = lines[0]?.startsWith('# ')
    ? lines.shift()?.slice(2).trim() || fallbackTitle
    : fallbackTitle

  const items = lines.map<DiagramItem>((line) => {
    const [itemTitle, ...detailParts] = line.split('|')
    const detail = detailParts.join('|').trim()

    return {
      title: itemTitle.trim(),
      detail: detail || undefined,
    }
  })

  return { items, title }
}

function renderInlineCode(value: string): ReactNode {
  return value.split(/(`[^`]+`)/g).map((part, index) => (
    part.startsWith('`') && part.endsWith('`')
      ? <code key={`${part}-${index}`}>{part.slice(1, -1)}</code>
      : <Fragment key={`${part}-${index}`}>{part}</Fragment>
  ))
}

const ProcessNodeCard = memo(function ProcessNodeCard({ data }: NodeProps<ProcessNode>) {
  return (
    <div className="process-react-node">
      {data.hasTarget && (
        <Handle
          className="process-react-node__handle"
          type="target"
          position={data.targetPosition}
          isConnectable={false}
        />
      )}
      <div className="process-react-node__meta">
        <span>{data.index}</span>
        <span>Stage</span>
      </div>
      <strong>{renderInlineCode(data.title)}</strong>
      {data.detail && <p>{renderInlineCode(data.detail)}</p>}
      {data.hasSource && (
        <Handle
          className="process-react-node__handle"
          type="source"
          position={data.sourcePosition}
          isConnectable={false}
        />
      )}
    </div>
  )
})

const nodeTypes = { process: ProcessNodeCard }

function useMobileDiagram() {
  const [mobile, setMobile] = useState(false)

  useEffect(() => {
    const query = window.matchMedia('(max-width: 760px)')
    const update = () => setMobile(query.matches)
    update()
    query.addEventListener('change', update)
    return () => query.removeEventListener('change', update)
  }, [])

  return mobile
}

function getPoints(count: number, mobile: boolean): Point[] {
  if (mobile) {
    return Array.from({ length: count }, (_, index) => ({ x: 0, y: index * 145 }))
  }

  if (count <= 4) {
    return Array.from({ length: count }, (_, index) => ({
      x: index * 215,
      y: 0,
    }))
  }

  return Array.from({ length: count }, (_, index) => {
    const row = Math.floor(index / 4)
    const positionInRow = index % 4
    const column = row % 2 === 0 ? positionInRow : 3 - positionInRow

    return { x: column * 215, y: row * 190 }
  })
}

function directionBetween(from: Point, to: Point) {
  if (to.y > from.y) return { source: Position.Bottom, target: Position.Top }
  if (to.x > from.x) return { source: Position.Right, target: Position.Left }
  return { source: Position.Left, target: Position.Right }
}

function buildGraph(items: DiagramItem[], mobile: boolean) {
  const points = getPoints(items.length, mobile)
  const nodes = items.map<ProcessNode>((item, index) => {
    const previousDirection = index > 0
      ? directionBetween(points[index - 1], points[index])
      : undefined
    const nextDirection = index < items.length - 1
      ? directionBetween(points[index], points[index + 1])
      : undefined

    return {
      id: `stage-${index + 1}`,
      type: 'process',
      position: points[index],
      draggable: false,
      selectable: false,
      data: {
        title: item.title,
        detail: item.detail,
        index: String(index + 1).padStart(2, '0'),
        hasTarget: index > 0,
        hasSource: index < items.length - 1,
        targetPosition: previousDirection?.target ?? Position.Left,
        sourcePosition: nextDirection?.source ?? Position.Right,
      },
    }
  })

  const edges = items.slice(0, -1).map<Edge>((_, index) => ({
    id: `edge-${index + 1}`,
    source: `stage-${index + 1}`,
    target: `stage-${index + 2}`,
    type: 'smoothstep',
    animated: true,
    focusable: false,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      width: 14,
      height: 14,
      color: '#af50ff',
    },
    style: { stroke: '#af50ff', strokeWidth: 1.25 },
  }))

  return { edges, nodes }
}

export function ProcessFlow({ source }: { source: string }) {
  const mobile = useMobileDiagram()
  const { items, title } = useMemo(() => parseDiagram(source, 'Process flow'), [source])
  const { edges, nodes } = useMemo(() => buildGraph(items, mobile), [items, mobile])
  const canvasHeight = mobile
    ? Math.min(900, Math.max(560, items.length * 145 + 80))
    : items.length <= 4 ? 270 : Math.ceil(items.length / 4) * 190 + 100

  return (
    <figure className="process-react-flow">
      <figcaption className="concept-diagram__header">
        <span>{title}</span>
        <span>{String(items.length).padStart(2, '0')} stages · interactive</span>
      </figcaption>
      <div className="process-react-flow__canvas" style={{ height: canvasHeight }}>
        <ReactFlow
          key={mobile ? 'mobile' : 'desktop'}
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          nodesDraggable={false}
          nodesConnectable={false}
          elementsSelectable={false}
          edgesFocusable={false}
          panOnDrag
          zoomOnScroll={false}
          zoomOnPinch
          zoomOnDoubleClick={false}
          preventScrolling={false}
          minZoom={0.55}
          maxZoom={1.4}
          fitView
          fitViewOptions={{ padding: mobile ? 0.1 : 0.05, maxZoom: 1.05 }}
          proOptions={{ hideAttribution: true }}
        >
          <Background
            id={`process-grid-${items.length}`}
            variant={BackgroundVariant.Dots}
            gap={22}
            size={1}
            color="rgba(247, 249, 250, 0.08)"
          />
          <Controls
            aria-label={`${title} 확대 및 이동 컨트롤`}
            position="bottom-right"
            orientation="horizontal"
            showInteractive={false}
          />
        </ReactFlow>
      </div>
    </figure>
  )
}

export function FactorGrid({ source }: { source: string }) {
  const { items, title } = parseDiagram(source, 'Key factors')

  return (
    <figure className="factor-grid">
      <figcaption className="concept-diagram__header">
        <span>{title}</span>
        <span>{String(items.length).padStart(2, '0')} factors</span>
      </figcaption>
      <ul>
        {items.map((item, index) => (
          <li key={`${item.title}-${index}`}>
            <span>{String(index + 1).padStart(2, '0')}</span>
            <strong>{renderInlineCode(item.title)}</strong>
            {item.detail && <p>{renderInlineCode(item.detail)}</p>}
          </li>
        ))}
      </ul>
    </figure>
  )
}
