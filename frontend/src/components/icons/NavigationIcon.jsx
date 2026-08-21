const PATHS = {
  home: <><path d="M3 10.8 12 3l9 7.8" /><path d="M5.5 9.5V21h13V9.5M9.5 21v-6h5v6" /></>,
  tasks: <><rect x="4" y="3" width="16" height="18" rx="3" /><path d="m8 9 1.5 1.5L12 8M14 9h3M8 15l1.5 1.5L12 14M14 15h3" /></>,
  expenses: <><path d="M5 7.5A2.5 2.5 0 0 1 7.5 5H19v14H7.5A2.5 2.5 0 0 1 5 16.5z" /><path d="M5 8h12M15 12h4" /><circle cx="15" cy="12" r=".7" fill="currentColor" stroke="none" /></>,
  projects: <><path d="M3.5 7.5h7l2-2h8v14h-17z" /><path d="M3.5 10h17" /></>,
  history: <><path d="M4.2 8.5A8 8 0 1 1 4 15" /><path d="M4 4v4.5h4.5M12 8v4l2.7 1.7" /></>,
  profile: <><circle cx="12" cy="8" r="3.5" /><path d="M5 20c.6-4 3-6 7-6s6.4 2 7 6" /></>,
  users: <><circle cx="9" cy="9" r="3" /><circle cx="17" cy="8" r="2.2" /><path d="M3.5 20c.5-4 2.4-6 5.5-6s5 2 5.5 6M15 13.5c3.2.2 4.8 2.2 5.2 5" /></>,
  signout: <><path d="M10 4H5v16h5M14 8l4 4-4 4M8 12h10" /></>,
}

function NavigationIcon({ name }) {
  return (
    <svg className="navigation-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      {PATHS[name] || PATHS.home}
    </svg>
  )
}

export default NavigationIcon
