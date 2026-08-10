import './NumberStepper.css'

interface NumberStepperProps {
  label: string
  max: number
  min?: number
  onChange: (value: number) => void
  value: number
}

export function NumberStepper({
  label,
  max,
  min = 1,
  onChange,
  value,
}: NumberStepperProps) {
  return (
    <div className="ui-stepper" role="group" aria-label={label}>
      <button
        type="button"
        className="ui-stepper__button"
        onClick={() => onChange(value - 1)}
        disabled={value <= min}
        aria-label={`${label} 줄이기`}
      >
        <span aria-hidden="true">−</span>
      </button>
      <output className="ui-stepper__value" aria-live="polite">
        {value}
      </output>
      <button
        type="button"
        className="ui-stepper__button"
        onClick={() => onChange(value + 1)}
        disabled={value >= max}
        aria-label={`${label} 늘리기`}
      >
        <span aria-hidden="true">+</span>
      </button>
    </div>
  )
}
