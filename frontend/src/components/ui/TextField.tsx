import { forwardRef, type InputHTMLAttributes } from 'react'
import './TextField.css'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: string
  helperText?: string
  label: string
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(
  function TextField(
    { className = '', error, helperText, id, label, ...props },
    ref,
  ) {
    const inputId = id ?? props.name
    const descriptionId = inputId ? `${inputId}-description` : undefined

    return (
      <label className="ui-field" htmlFor={inputId}>
        <span className="ui-field__label">{label}</span>
        <input
          {...props}
          ref={ref}
          id={inputId}
          className={`ui-field__input ${error ? 'ui-field__input--error' : ''} ${className}`.trim()}
          aria-invalid={Boolean(error)}
          aria-describedby={error || helperText ? descriptionId : undefined}
        />
        {(error || helperText) && (
          <span
            id={descriptionId}
            className={`ui-field__description ${error ? 'ui-field__description--error' : ''}`.trim()}
          >
            {error ?? helperText}
          </span>
        )}
      </label>
    )
  },
)
