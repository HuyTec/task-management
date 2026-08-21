import appIcon from '../../assets/icon.png'

function AppLogo({ admin = false, className = '' }) {
  const classes = `${className}${admin ? ' brand-mark--admin' : ' brand-mark--logo'}`.trim()

  return (
    <span className={classes} aria-hidden="true">
      {admin ? 'AD' : <img src={appIcon} alt="" />}
    </span>
  )
}

export default AppLogo
