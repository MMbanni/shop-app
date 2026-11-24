import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useAuth } from "../context/AuthContext";
import { api } from "../lib/api";
import { getErrorMessage, getFieldErrors } from "../lib/ApiError";
import { money } from "../lib/money";
import "./AccountPage.css";
import { FloatingMessage } from "../components/messages/FloatingMessage";
import { ButtonMessage } from "../types/components";

type FormProps = {
  children: ReactNode;
  button: string;
  success: string;
  save: (fields: FormData) => Promise<unknown>;
};

function AccountForm({ children, button, success, save }: FormProps) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  const messageTimer = useRef<number | undefined>(undefined);

  const [message, setMessage] = useState<ButtonMessage | null>(null);
  const [messageVisible, setMessageVisible] = useState<boolean>(false);

  useEffect(() => {
    return () => window.clearTimeout(messageTimer.current);
  }, []);

  function showMessage(message: ButtonMessage) {
    window.clearTimeout(messageTimer.current);

    setMessage(message);
    setMessageVisible(true);

    messageTimer.current = window.setTimeout(() => {
      setMessageVisible(false);
    }, 3000);
  }

  const mutation = useMutation({
    mutationFn: save,
    onSuccess: () => {
      if (buttonRef.current) {
        showMessage({ text: success, anchor: buttonRef.current })
      }
    }
  });

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!mutation.isPending) {
      setMessageVisible(false);
      mutation.mutate(new FormData(event.currentTarget));
    }
  }

  return (
    <form
      onSubmit={submit}
      onChange={() => {
        mutation.reset();
        setMessageVisible(false);
      }}>


      <fieldset disabled={mutation.isPending}>
        {children}

        <button ref={buttonRef} type="submit" className="button">
          {mutation.isPending ? "Please wait…" : button}
        </button>
      </fieldset>

      {mutation.isSuccess && <p role="status">{success}</p>}
      {mutation.isError && <p role="alert">{getFieldErrors(mutation.error).name}</p>}
    </form>
  );
}

export function AccountPage() {
  const { user } = useAuth();
  return user ? <Account key={user.id} id={user.id} /> : null;
}

function Account({ id }: { id: number }) {

  const [page, setPage] = useState(0);
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const profile = useQuery({ queryKey: ["account", id], queryFn: api.me });
  const orders = useQuery({ queryKey: ["customer-orders", id, page], queryFn: () => api.myOrders(page) });

  return <main className="page-shell narrow account-page">
    <h1>My account</h1>

    <div className="account-columns">
      <section>
        <h2>Profile</h2>
        {profile.isPending && <p>Loading profile…</p>}
        {profile.isError && <p role="alert">{getErrorMessage(profile.error)}</p>}
        {profile.data &&
          <AccountForm
            button="Save name"
            success="Name saved."
            save={async fields => {
              await api.saveName(String(fields.get("name")).trim());
              await profile.refetch();
            }}
          >
            <label> Name
              <input name="name"
                defaultValue={profile.data.name} autoComplete="name" required minLength={2} maxLength={50} />
            </label>
            <p>Current email<br /><strong>{profile.data.email}</strong></p>
          </AccountForm>}
      </section>

      <section><h2>Change email</h2>
        <AccountForm button="Send verification code" success="Check your new inbox. Your code expires in 15 minutes." save={async fields => {
          await api.requestEmail(String(fields.get("email")).trim(), password);
          setPassword("");
        }}>
          <label>New email<input name="email" type="email" maxLength={254} required /></label>
          <label>Current password<input type="password" autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} /></label>
        </AccountForm>
      </section>
      <section><h2>Verify new email</h2>
        <AccountForm button="Confirm email" success="Email verified. Use your new email next time you sign in." save={async () => {
          await api.confirmEmail(code.trim());
          setCode("");
          await profile.refetch();
        }}>
          <label>Code from your new inbox<input required maxLength={100} autoComplete="one-time-code" value={code} onChange={e => setCode(e.target.value)} /></label>
        </AccountForm>
      </section>
    </div>

    <section>
      <h2>Order history</h2>
      {orders.isPending && <p>Loading orders…</p>}
      {orders.isError && <p role="alert">{getErrorMessage(orders.error)}</p>}
      {orders.data?.length === 0 && <p>{page === 0 ? "You have no orders yet." : "No older orders."}</p>}

      <ul>{orders.data?.map(order => <li key={order.id}>
        <strong>Order #{order.id}</strong>
        <span>{new Date(order.createdAt).toLocaleDateString()}</span>
        <span>{order.status}</span><strong>{money(order.total)}</strong>
      </li>)}</ul>
      {page > 0 && <button className="button ghost" onClick={() => setPage(page - 1)}>Newer orders</button>}
      {orders.data?.length === 10 && <button className="button ghost" onClick={() => setPage(page + 1)}>Older orders</button>}
    </section>
  </main>;
}
