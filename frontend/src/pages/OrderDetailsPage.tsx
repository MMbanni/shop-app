import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { api } from "../lib/api";
import { money } from "../lib/money";
import { getErrorMessage } from "../lib/ApiError";
import { useAuth } from "../context/AuthContext";
import "./OrderDetailsPage.css"

export function OrderDetailsPage() {
  const { id = "" } = useParams();
  const { user } = useAuth();

  const order = useQuery({
    queryKey: ["order-details", user?.id, id],
    queryFn: () => api.orderDetails(id),
    enabled: /^\d+$/.test(id)
  });


  if (!/^\d+$/.test(id)) {
    return <main className="page-shell narrow"><p role="alert">Invalid order ID.</p></main>;
  }

  return (
    <main className="page-shell narrow order-details">
      <Link to="/account">← My account</Link>
      <h1>Order #{id}</h1>
      {order.isPending &&
        <p role="status">Loading order…</p>
      }

      {order.isError &&
        <p role="alert">{getErrorMessage(order.error)}</p>
      }

      { order.data &&
        <>
          <div className="order-meta">
            <span className="status-badge">
              {order.data.status}
            </span>
            <span>Placed {new Date(order.data.createdAt).toLocaleString()}</span>

            {order.data.paidAt &&
              <span>Paid {new Date(order.data.paidAt).toLocaleString()}
              </span>}
          </div>

          <p>Items and prices recorded when this order was placed.</p>

          <div className="table-scroll">
            <table>
              <caption className="sr-only">Purchased items</caption>
              <thead>
                <tr>
                  <th scope="col">Product</th>
                  <th scope="col">Quantity</th>
                  <th scope="col">Unit price</th>
                  <th scope="col">Total</th>
                </tr>
              </thead>
              <tbody>{order.data.items.map((item, index) =>
                <tr key={index}>
                  <td>{item.name}</td>
                  <td>{item.quantity}</td>
                  <td>{money(item.unitPrice)}</td>
                  <td>{money(item.lineTotal)}</td>
                </tr>)}
              </tbody>
              <tfoot>
                <tr>
                  <th colSpan={3} scope="row">Order total</th>
                  <td><strong>{money(order.data.total)}</strong></td>
                </tr>
              </tfoot>
            </table>
          </div></>
      }
    </main>);
}
