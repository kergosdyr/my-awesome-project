import type { ReactNode } from 'react'
import './StatePanel.css'

type StateKind = 'empty' | 'error' | 'loading'

interface StatePanelProps {
  action?: ReactNode
  description: string
  kind: StateKind
  title: string
}

export function StatePanel({
  action,
  description,
  kind,
  title,
}: StatePanelProps) {
  const role = kind === 'error' ? 'alert' : 'status'

  return (
    <div className={`ui-state ui-state--${kind}`} role={role}>
      <strong className="ui-state__title">{title}</strong>
      <p className="ui-state__description">{description}</p>
      {action && <div className="ui-state__action">{action}</div>}
    </div>
  )
}
