'use client'

import { Bar, BarChart, CartesianGrid, LabelList, ReferenceDot, XAxis, YAxis } from 'recharts'
import { ChartContainer, ChartTooltip, type ChartConfig } from '@/components/ui/chart'

type ChartData = {
  title: string
  description?: string
  unit: string
  data: { label: string; value: number }[]
}

function parseChart(source: string): ChartData | undefined {
  try {
    const v = JSON.parse(source)
    if (typeof v.title !== 'string' || typeof v.unit !== 'string'
      || (v.description !== undefined && typeof v.description !== 'string')
      || !Array.isArray(v.data) || v.data.length < 1 || v.data.length > 20
      || !v.data.every((r: { label?: unknown; value?: unknown }) =>
        typeof r.label === 'string' && typeof r.value === 'number'
        && Number.isFinite(r.value) && r.value >= 0)) return undefined
    return v
  } catch { return undefined }
}

export function ArticleChart({ source }: { source: string }) {
  const chart = parseChart(source)
  if (!chart) return <pre><code>{source}</code></pre>
  const config = { value: { label: chart.unit, color: 'var(--lavender)' } } satisfies ChartConfig
  return (
    <figure className="article-chart">
      <figcaption><strong>{chart.title}</strong>{chart.description && <p>{chart.description}</p>}</figcaption>
      <ChartContainer config={config} style={{ height: Math.max(220, chart.data.length * 52), width: '100%' }}>
        <BarChart accessibilityLayer data={chart.data} layout="vertical" margin={{ left: 0, right: 60 }}>
          <CartesianGrid horizontal={false} stroke="var(--line-strong)" />
          <YAxis dataKey="label" type="category" width={110} tickLine={false} axisLine={false} tick={{ fill: 'var(--white-soft)', fontSize: 12 }} />
          <XAxis type="number" domain={[0, 'auto']} tickLine={false} axisLine={false} tick={{ fill: 'var(--ash)', fontSize: 12 }} unit={chart.unit} />
          <ChartTooltip cursor={{ fill: 'var(--line)' }} content={({ active, payload }) => active && payload?.length ? (
            <div className="article-chart-tooltip">{String(payload[0].payload.label)}<strong>{Number(payload[0].value).toLocaleString('ko-KR', { maximumFractionDigits: 4 })}{chart.unit}</strong></div>
          ) : null} />
          <Bar dataKey="value" fill="var(--color-value)" radius={[0, 4, 4, 0]} maxBarSize={24} isAnimationActive={false}>
            <LabelList dataKey="value" position="right" offset={8} fill="var(--white)" fontSize={12}
              formatter={(value) => `${Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}${chart.unit}`} />
          </Bar>
          {chart.data.filter((row) => row.value === 0).map((row, index) => (
            <ReferenceDot key={index} x={0} y={row.label} r={0}
              label={{ value: `0${chart.unit}`, position: 'right', offset: 8, fill: 'var(--white)', fontSize: 12 }} />
          ))}
        </BarChart>
      </ChartContainer>
    </figure>
  )
}
