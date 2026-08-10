import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'
import './Button.css'

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'danger'
type ButtonSize = 'small' | 'medium'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode
  loading?: boolean
  loadingLabel?: string
  size?: ButtonSize
  variant?: ButtonVariant
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  function Button(
    {
      children,
      className = '',
      disabled,
      loading = false,
      loadingLabel = '처리 중',
      size = 'medium',
      variant = 'secondary',
      type = 'button',
      ...props
    },
    ref,
  ) {
    return (
      <button
        {...props}
        ref={ref}
        type={type}
        className={`ui-button ui-button--${variant} ui-button--${size} ${className}`.trim()}
        disabled={disabled || loading}
        aria-busy={loading || undefined}
      >
        {loading ? loadingLabel : children}
      </button>
    )
  },
)
