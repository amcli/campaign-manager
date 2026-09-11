import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { auth } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { DiceIcon, Field } from '../components/ui';

export function LoginPage({ mode }: { mode: 'login' | 'register' }) {
  const isRegister = mode === 'register';
  const { login, register, devLogin } = useAuth();
  const { notifyError } = useToast();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [devLoginEnabled, setDevLoginEnabled] = useState(false);

  useEffect(() => {
    auth.devLoginEnabled()
      .then((status) => setDevLoginEnabled(status.enabled))
      .catch(() => setDevLoginEnabled(false));
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      if (isRegister) {
        await register(username, email, password);
      } else {
        await login(username, password);
      }
      navigate('/');
    } catch (error) {
      notifyError(error);
    } finally {
      setBusy(false);
    }
  }

  async function continueAsDev() {
    try {
      await devLogin();
      navigate('/');
    } catch (error) {
      notifyError(error);
    }
  }

  return (
    <div className="auth-wrap">
      <div className="panel auth-box">
        <div className="crest">
          <DiceIcon />
        </div>
        <h1>{isRegister ? 'Join the table' : 'Welcome back'}</h1>
        <p className="tagline">
          {isRegister ? 'Create an account to run campaigns and build characters.' : 'Log in to your campaigns and characters.'}
        </p>
        <form className="stack" onSubmit={submit}>
          <Field label="Username">
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} required autoComplete="username" />
          </Field>
          {isRegister && (
            <Field label="Email">
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
            </Field>
          )}
          <Field label="Password">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={isRegister ? 8 : 1}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
            />
          </Field>
          <div className="actions">
            <button type="submit" className="button" disabled={busy}>
              {isRegister ? 'Create account' : 'Log in'}
            </button>
            <span className="muted small">
              {isRegister ? (
                <>Already have an account? <Link to="/login">Log in</Link></>
              ) : (
                <>New here? <Link to="/register">Create an account</Link></>
              )}
            </span>
          </div>
        </form>
        {!isRegister && devLoginEnabled && (
          <>
            <hr className="divider" />
            <button className="button ghost" style={{ width: '100%', justifyContent: 'center' }} onClick={continueAsDev}>
              Continue as dev (local only)
            </button>
          </>
        )}
      </div>
    </div>
  );
}
