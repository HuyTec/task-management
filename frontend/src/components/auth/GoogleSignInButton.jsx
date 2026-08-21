import { useEffect, useRef, useState } from 'react'
import { GoogleIcon } from '../icons/GoogleIcon'

const GOOGLE_SCRIPT_ID = 'google-identity-services'
const GOOGLE_SCRIPT_URL = 'https://accounts.google.com/gsi/client'

let googleScriptPromise

function loadGoogleIdentityServices() {
  if (window.google?.accounts?.id) return Promise.resolve(window.google)
  if (googleScriptPromise) return googleScriptPromise

  googleScriptPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(GOOGLE_SCRIPT_ID)
    const script = existingScript || document.createElement('script')

    const handleLoad = () => resolve(window.google)
    const handleError = () => {
      googleScriptPromise = undefined
      reject(new Error('Google sign-in could not be loaded. Please try again.'))
    }

    script.addEventListener('load', handleLoad, { once: true })
    script.addEventListener('error', handleError, { once: true })

    if (!existingScript) {
      script.id = GOOGLE_SCRIPT_ID
      script.src = GOOGLE_SCRIPT_URL
      script.async = true
      document.head.appendChild(script)
    }
  })

  return googleScriptPromise
}

function GoogleSignInButton({ disabled = false, onCredential, onError }) {
  const buttonContainerRef = useRef(null)
  const callbackRef = useRef(onCredential)
  const errorCallbackRef = useRef(onError)
  const [loadError, setLoadError] = useState('')
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim()

  useEffect(() => {
    callbackRef.current = onCredential
    errorCallbackRef.current = onError
  }, [onCredential, onError])

  useEffect(() => {
    if (!clientId || !buttonContainerRef.current) return undefined

    const buttonContainer = buttonContainerRef.current
    let isActive = true
    let lastRenderedWidth = 0
    let resizeObserver

    loadGoogleIdentityServices()
      .then((google) => {
        if (!isActive) return

        google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => {
            if (response?.credential) callbackRef.current(response.credential)
            else errorCallbackRef.current('Google did not return an identity token.')
          },
        })

        const renderButton = () => {
          const width = Math.max(200, Math.min(400, Math.floor(buttonContainer.clientWidth)))
          if (width === lastRenderedWidth) return
          lastRenderedWidth = width
          buttonContainer.replaceChildren()
          google.accounts.id.renderButton(buttonContainer, {
            type: 'standard',
            theme: 'outline',
            size: 'large',
            text: 'continue_with',
            shape: 'rectangular',
            width,
          })
        }

        renderButton()
        resizeObserver = new window.ResizeObserver(renderButton)
        resizeObserver.observe(buttonContainer)
      })
      .catch((error) => {
        if (!isActive) return
        const message = error.message || 'Google sign-in could not be loaded.'
        setLoadError(message)
        errorCallbackRef.current(message)
      })

    return () => {
      isActive = false
      resizeObserver?.disconnect()
      buttonContainer.replaceChildren()
    }
  }, [clientId])
  
  if (!clientId) {
    return (
      <div className="google-sign-in__configuration" role="status">
        <GoogleIcon />
        <div>
          <strong>Google sign-in is not configured</strong>
          <span>Set VITE_GOOGLE_CLIENT_ID for this environment.</span>
        </div>
      </div>
    )
  }

  if (loadError) {
    return <p className="google-sign-in__status">Google sign-in is temporarily unavailable.</p>
  }

  return (
    <div
      className={`google-sign-in${disabled ? ' google-sign-in--disabled' : ''}`}
      ref={buttonContainerRef}
      aria-busy={disabled}
    />
  )
}

export default GoogleSignInButton
