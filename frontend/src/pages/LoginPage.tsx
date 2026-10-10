import { useState, type FormEvent } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Navigate } from "react-router";
import type { RootState } from "@/store/store";
import { setSession } from "@/store/Slices/authSlice";
import * as tutoring from "@/services/tutoring";

export default function LoginPage() {
  const dispatch = useDispatch();
  const { user } = useSelector((state: RootState) => state.auth);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setError("");
    try {
      dispatch(
        setSession(
          await tutoring.login(
            String(data.get("username")),
            String(data.get("password")),
          ),
        ),
      );
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Login failed.");
    } finally {
      setBusy(false);
    }
  }

  if (user)
    return (
      <Navigate
        to={user.role === "TUTOR" ? "/provider" : "/customer"}
        replace
      />
    );
  return (
    <section>
      <h2>Login</h2>
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      <form onSubmit={(event) => void login(event)}>
        <label>
          Username or email
          <input
            name="username"
            required
            maxLength={100}
            autoComplete="username"
          />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            required
            maxLength={72}
            autoComplete="current-password"
          />
        </label>
        <button disabled={busy}>Log in</button>
      </form>
    </section>
  );
}
