import { useState } from 'react'

function PasswordField({
  id = 'password',
  label = 'Password',
  value,
  onChange,
  autoComplete,
  placeholder = 'Enter your password',
  required = true,
  description,
  error,
}) {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)

  return (
    <div className="field-group">
      <div className="field-label-row">
        <label htmlFor={id}>{label}</label>
        <button
          className="text-button"
          type="button"
          aria-pressed={isPasswordVisible}
          onClick={() => setIsPasswordVisible((currentValue) => !currentValue)}
        >
          {isPasswordVisible ? 'Hide password' : 'Show password'}
        </button>
      </div>
      <input
        id={id}
        name="password"
        type={isPasswordVisible ? 'text' : 'password'}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoComplete={autoComplete}
        minLength="8"
        aria-describedby={error ? `${id}-error` : description ? `${id}-description` : undefined}
        aria-invalid={Boolean(error)}
        required={required}
      />
      {description && !error && <small className="field-help" id={`${id}-description`}>{description}</small>}
      {error && <small className="field-error" id={`${id}-error`}>{error}</small>}
    </div>
  )
}

export default PasswordField
