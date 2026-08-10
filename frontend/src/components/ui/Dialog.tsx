import {
  useEffect,
  useId,
  useRef,
  type KeyboardEvent,
  type ReactNode,
  type RefObject,
} from 'react'
import './Dialog.css'

interface DialogProps {
  children: ReactNode
  description?: string
  footer?: ReactNode
  initialFocusRef?: RefObject<HTMLElement | null>
  onClose: () => void
  returnFocusRef?: RefObject<HTMLElement | null>
  title: string
}

const focusableSelector = [
  'button:not([disabled])',
  'a[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export function Dialog({
  children,
  description,
  footer,
  initialFocusRef,
  onClose,
  returnFocusRef,
  title,
}: DialogProps) {
  const titleId = useId()
  const descriptionId = useId()
  const surfaceRef = useRef<HTMLDivElement>(null)
  const closeButtonRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    const returnFocusTarget = returnFocusRef?.current
    document.body.style.overflow = 'hidden'
    const focusTarget = initialFocusRef?.current ?? closeButtonRef.current
    focusTarget?.focus()

    return () => {
      document.body.style.overflow = previousOverflow
      returnFocusTarget?.focus()
    }
  }, [initialFocusRef, returnFocusRef])

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault()
      onClose()
      return
    }

    if (event.key !== 'Tab') return

    const focusable = Array.from(
      surfaceRef.current?.querySelectorAll<HTMLElement>(focusableSelector) ?? [],
    )
    const first = focusable[0]
    const last = focusable[focusable.length - 1]

    if (!first || !last) {
      event.preventDefault()
      return
    }

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return (
    <div className="ui-dialog__backdrop" onMouseDown={onClose}>
      <div
        ref={surfaceRef}
        className="ui-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        onKeyDown={handleKeyDown}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="ui-dialog__header">
          <div className="ui-dialog__heading">
            <h2 id={titleId}>{title}</h2>
            {description && <p id={descriptionId}>{description}</p>}
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            className="ui-dialog__close"
            onClick={onClose}
            aria-label="대화상자 닫기"
          >
            <span aria-hidden="true">×</span>
          </button>
        </header>
        <div className="ui-dialog__content">{children}</div>
        {footer && <footer className="ui-dialog__footer">{footer}</footer>}
      </div>
    </div>
  )
}
