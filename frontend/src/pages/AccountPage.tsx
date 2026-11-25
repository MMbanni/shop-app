import { useState, type FormEvent, type ReactNode } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";
import { getErrorMessage, getFieldErrors } from "../lib/ApiError";
import { money } from "../lib/money";
import "./AccountPage.css";
import { Link } from "react-router-dom";


type FormProps = {
  children: ReactNode;
  button: string;
  success?: string;
  primary?: boolean;
  save: (fields: FormData) => Promise<unknown>;
};

function AccountForm({ children, button, success, primary = false, save }: FormProps) {

  const mutation = useMutation({ mutationFn: save });

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!mutation.isPending) {
      mutation.mutate(new FormData(event.currentTarget));
    }
  }

  const errorMessage = mutation.isError
    ? Object.values(getFieldErrors(mutation.error)).find(Boolean)
      ?? getErrorMessage(mutation.error)
    : null;

  return (
    <form
      className="account-form"
      onSubmit={submit}
      onChange={() => mutation.reset()}
    >
      <fieldset disabled={mutation.isPending}>
        {children}

        <button
          type="submit"
          className={`button${primary ? "" : " ghost"}`}
        >
          {mutation.isPending ? "Please wait…" : button}
        </button>
      </fieldset>

      {mutation.isSuccess && success && (
        <p className="account-feedback account-success" role="status">
          {success}
        </p>
      )}

      {errorMessage && (
        <p className="account-feedback" role="alert">
          {errorMessage}
        </p>
      )}
    </form>
  );
}

export function AccountPage() {
  const { user } = useAuth();
  return user ? <Account key={user.id} id={user.id} /> : null;
}

function Account({ id }: { id: number }) {

  const [page, setPage] = useState(0);
  const [newEmail, setNewEmail] = useState("");
  const [pendingEmail, setPendingEmail] = useState<string | null>(null);
  const [emailNotice, setEmailNotice] = useState("");
  const [requestHint, setRequestHint] = useState("");

  const profile = useQuery({
    queryKey: ["account", id],
    queryFn: api.me,
  });


  const orders = useQuery({
    queryKey: ["customer-orders", id, page],
    queryFn: () => api.myOrders(page),
  });

  return (
    <main className="page-shell narrow account-page">
      <header className="account-heading">
        <h1>My account</h1>
        <p>Manage your details and view your orders.</p>
      </header>


      <div className="account-columns">
        <section className="account-card">
          <h2>Profile</h2>

          {profile.isPending && <p role="status">Loading profile…</p>}

          {profile.isError && (
            <p role="alert">{getErrorMessage(profile.error)}</p>
          )}

          {profile.data && (
            <>
              <div className="account-email">
                <span>Current email</span>
                <strong>{profile.data.email}</strong>
              </div>

              <AccountForm
                button="Save name"
                success="Name saved."
                save={async fields => {
                  await api.saveName(String(fields.get("name")).trim());
                  await profile.refetch();
                }}
              >
                <label>
                  Name
                  <input
                    name="name"
                    defaultValue={profile.data.name}
                    autoComplete="name"
                    required
                    minLength={2}
                    maxLength={50}
                  />
                </label>
              </AccountForm>
            </>
          )}

        </section>

        <section className="account-card">
          <h2>Change email</h2>

          {emailNotice && (
            <p className="account-feedback account-success" role="status">
              {emailNotice}
            </p>
          )}

          {pendingEmail === null ? (
            <>
              <p className="account-help">
                Your current email stays active until you verify the new one.
              </p>

              {requestHint && (
                <p className="account-help" role="status">
                  {requestHint}
                </p>
              )}

              <AccountForm
                key="request-email"
                button="Send verification code"
                primary
                save={async fields => {
                  const target = String(fields.get("newEmail")).trim();
                  const password = String(fields.get("password"));

                  await api.requestEmail(target, password);

                  setNewEmail(target);
                  setPendingEmail(target);
                  setEmailNotice("");
                  setRequestHint("");
                }}
              >
                <label>
                  New email
                  <input
                    name="newEmail"
                    type="email"
                    autoComplete="off"
                    value={newEmail}
                    onChange={event => setNewEmail(event.target.value)}
                    maxLength={254}
                    required
                  />
                </label>

                <label>
                  Current password
                  <input
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    required
                  />
                </label>
              </AccountForm>
            </>
          ) : (
            <>
              <div className="account-verification" role="status">
                <h3>Check your new inbox</h3>
                <p>
                  We sent a verification code to{" "}
                  <strong>{pendingEmail}</strong>.
                </p>
                <p>
                  Codes are valid for 15 minutes. If you request another,
                  use the latest code.
                </p>
              </div>

              <AccountForm
                key="confirm-email"
                button="Confirm new email"
                primary
                save={async fields => {
                  await api.confirmEmail(
                    String(fields.get("code")).trim()
                  );

                  setPendingEmail(null);
                  setNewEmail("");
                  setEmailNotice(
                    "Email verified. Use your new email next time you sign in."
                  );

                  await profile.refetch();
                }}
              >
                <label>
                  Verification code
                  <input
                    name="code"
                    autoComplete="one-time-code"
                    spellCheck={false}
                    maxLength={100}
                    required
                    autoFocus
                  />
                </label>

                <div className="account-email-actions">
                  <button
                    type="button"
                    className="account-text-button"
                    onClick={() => {
                      setPendingEmail(null);
                      setRequestHint(
                        "Re-enter your current password to send a fresh code."
                      );
                    }}
                  >
                    Resend code
                  </button>

                  <button
                    type="button"
                    className="account-text-button"
                    onClick={() => {
                      setPendingEmail(null);
                      setRequestHint("Enter the email address you want to use.");
                    }}
                  >
                    Use another email
                  </button>
                </div>
              </AccountForm>
            </>
          )}
        </section>
      </div>

      <section className="account-card account-orders">
        <h2>Order history</h2>

        {orders.isPending && <p role="status">Loading orders…</p>}

        {orders.isError && (
          <p role="alert">{getErrorMessage(orders.error)}</p>
        )}

        {orders.data?.length === 0 && (
          <div className="account-empty">
            <p>
              { page === 0
                ? "You have no orders yet."
                : "No older orders." }
            </p>

            {page === 0 && (
              <Link className="account-text-button" to="/products">
                Browse products
              </Link>
            )}
          </div>
        )}

        {!!orders.data?.length && (
          <ul className="account-order-list">
            {orders.data.map(order => (
              <li key={order.id}>
                <div>
                  <Link to={`/account/orders/${order.id}`}>Order #{order.id} →</Link>
                  <time dateTime={order.createdAt}>
                    {new Date(order.createdAt).toLocaleDateString()}
                  </time>
                </div>

                <span className="account-order-status">
                  {order.status}
                </span>

                <strong>{money(order.total)}</strong>
              </li>
            ))}
          </ul>
        )}

        {(page > 0 || orders.data?.length === 10) && (
          <nav className="account-pagination" aria-label="Order history pages">
            <button
              className="button ghost"
              disabled={page === 0 || orders.isFetching}
              onClick={() => setPage(current => current - 1)}
            >
              Newer orders
            </button>

            <span>Page {page + 1}</span>

            <button
              className="button ghost"
              disabled={orders.data?.length !== 10 || orders.isFetching}
              onClick={() => setPage(current => current + 1)}
            >
              Older orders
            </button>
          </nav>
        )}
      </section>

    </main>
  );
}