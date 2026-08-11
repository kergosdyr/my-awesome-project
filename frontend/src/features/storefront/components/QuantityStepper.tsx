import { MinusIcon, PlusIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ButtonGroup, ButtonGroupText } from '@/components/ui/button-group'

interface QuantityStepperProps {
  label: string
  max: number
  min?: number
  onChange: (value: number) => void
  value: number
}

export function QuantityStepper({
  label,
  max,
  min = 1,
  onChange,
  value,
}: QuantityStepperProps) {
  return (
    <ButtonGroup aria-label={label}>
      <Button
        type="button"
        variant="outline"
        size="icon-sm"
        onClick={() => onChange(value - 1)}
        disabled={value <= min}
        aria-label={`${label} 줄이기`}
      >
        <MinusIcon />
      </Button>
      <ButtonGroupText asChild>
        <output className="min-w-9 justify-center tabular-nums" aria-live="polite">
          {value}
        </output>
      </ButtonGroupText>
      <Button
        type="button"
        variant="outline"
        size="icon-sm"
        onClick={() => onChange(value + 1)}
        disabled={value >= max}
        aria-label={`${label} 늘리기`}
      >
        <PlusIcon />
      </Button>
    </ButtonGroup>
  )
}
