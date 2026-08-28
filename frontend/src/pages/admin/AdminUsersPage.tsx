import { useQuery } from "@tanstack/react-query";
import { api } from "../../lib/api";
import { money } from "../../lib/money";
import { BackToAdminButton } from "../../components/buttons/BackToAdminButton";

export function AdminUsersPage() {
  const { data: users, isLoading, isError, error } = useQuery({
    queryKey: ["users"],
    queryFn: api.users
  });

  if (isLoading) {
    return <p className="page-message">Loading users...</p>;
  }

  if (isError) {
    return <p className="page-message error">{error.message}</p>;
  }

  return (
    <main className="page-shell narrow">
      <div className="page-heading">
        <p className="section-label">Admin</p>
        <h1>Users</h1>
      </div>

      <div className="table-card">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
            </tr>
          </thead>
          <tbody>
            {users?.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.role}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
  
      <BackToAdminButton>
              
            </BackToAdminButton>

    </main>
  );
}
