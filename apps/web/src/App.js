import './App.css';

function App() {
  const currentPath = window.location.pathname.toLowerCase();
  const isPrivacyPage = currentPath === '/privacy-policy';

  return (
    <div className="app">
      {isPrivacyPage ? (
        <main className="privacy-page" aria-label="Privacy policy page">
          <article className="privacy-card">
            <h1>Privacy Policy</h1>
            <p className="effective-date">Effective date: March 21, 2026</p>

            <section>
              <h2>Overview</h2>
              <p>
                Offline UPI is designed to help users access UPI-related workflows while
                minimizing data collection. We are committed to handling information in a
                clear and responsible way.
              </p>
            </section>

            <section>
              <h2>Information We Collect</h2>
              <p>
                The app may process device-level information required for core functionality,
                such as app preferences and basic diagnostic logs. We do not intentionally
                collect unnecessary personal data.
              </p>
            </section>

            <section>
              <h2>How Information Is Used</h2>
              <p>
                Data is used to improve app reliability, maintain performance, and support
                user-requested features. Information is not sold to third parties.
              </p>
            </section>

            <section>
              <h2>Data Storage and Security</h2>
              <p>
                Where possible, data remains on your device. We apply reasonable safeguards
                to protect any stored information from unauthorized access.
              </p>
            </section>

            <section>
              <h2>Your Choices</h2>
              <p>
                You can uninstall the app at any time to remove locally stored app data.
                Contact support if you need help with privacy-related requests.
              </p>
            </section>

            <section>
              <h2>Contact</h2>
              <p>
                For privacy questions, email: <a href="mailto:guptashaurya2002@gmail.com">guptashaurya2002@gmail.com</a>
              </p>
            </section>
          </article>
        </main>
      ) : (
        <main className="home-page" aria-label="Home page">
          <div className="home-card">
            <h1>Offline UPI</h1>
            <p>
              This app supports offline-friendly UPI workflows for faster, simpler payments.
            </p>
            <a className="policy-link" href="/privacy-policy">
              View Privacy Policy
            </a>
          </div>
        </main>
      )}
    </div>
  );
}

export default App;
