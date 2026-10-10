import { useEffect, useState } from "react";
import { Link, Navigate, Outlet } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import type { RootState } from "@/store/store";
import { setSession } from "@/store/Slices/authSlice";
import * as tutoring from "@/services/tutoring";
import "./pages.scss";

export default function Layout() {
  const dispatch = useDispatch();
  const { user } = useSelector((state: RootState) => state.auth);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    tutoring
      .getSession()
      .then((session) => {
        if (active) {
          dispatch(setSession(session));
          setReady(true);
        }
      })
      .catch((cause: Error) => {
        if (active) setError(cause.message);
      });
    return () => {
      active = false;
    };
  }, [dispatch]);

  async function logout() {
    try {
      dispatch(setSession(await tutoring.logout()));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Logout failed.");
    }
  }

  return (
    <main className="home-page">
      <header className="page-header">
        <h1>Tutoring appointments</h1>
        {user && (
          <div>
            {user.name} <button onClick={() => void logout()}>Log out</button>
          </div>
        )}
      </header>
      {user && (
        <nav className="actions">
          {user.role === "TUTOR" ? (
            <>
              <Link to="/provider">Provider portal</Link>
              <Link to="/provider/slots">Manage slots</Link>
            </>
          ) : (
            <>
              <Link to="/customer">Available slots</Link>
              <Link to="/customer/appointments">My appointments</Link>
            </>
          )}
        </nav>
      )}
      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {ready ? <Outlet /> : <p>Loading…</p>}
      {!ready && error && (
        <button onClick={() => window.location.reload()}>Try again</button>
      )}
    </main>
  );
}

export function RequireRole({ role }: { role: "STUDENT" | "TUTOR" }) {
  const { user } = useSelector((state: RootState) => state.auth);
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role)
    return (
      <Navigate
        to={user.role === "TUTOR" ? "/provider" : "/customer"}
        replace
      />
    );
  return <Outlet />;
}

export function StartPage() {
  const { user } = useSelector((state: RootState) => state.auth);
  return (
    <Navigate
      to={!user ? "/login" : user.role === "TUTOR" ? "/provider" : "/customer"}
      replace
    />
  );
}
