import { useState } from "react";

export function ContactPage() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    console.log({
      email,
      message,
    });

    // TODO: send to API
  }

  return (
    <main className="page-shell narrow">
      <div className="page-heading">
        <p className="section-label">Contact us</p>
        <p className="muted">
          Have a question? Send us a message and we'll get back to you.
        </p>
      </div>

      <form className="contact-form" onSubmit={handleSubmit}>
        <label>
          Your email

          <input
            type="email"
            name="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@email.com"
          />
        </label>

        <label>
          Message

          <textarea
            name="message"
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            placeholder="How can we help?"
            rows={7}
          />
        </label>

        <button className="button" type="submit">
          Send message
        </button>
      </form>
    </main>
  );
}