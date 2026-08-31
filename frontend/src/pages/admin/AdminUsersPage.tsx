import { BackToAdminButton } from "../../components/buttons/BackToAdminButton";
import type { AdminUserTab, ApiErrorResponse, UserStatus } from "../../types";
import { useState} from "react";
import { useAdminUsers } from "../../hooks/useAdminUserActions";
import { UserFormModal } from "../../components/admin/UserFormModal";


const tabs: AdminUserTab[] = ["ALL", "ACTIVE", "INACTIVE", "SUSPENDED", "BANNED"];


export function AdminUsersPage() {

  const [selectedTab, setSelectedTab] = useState<AdminUserTab>("ALL");
  // Leave for error handling update
  const [errorResponse, setErrorResponse] = useState<ApiErrorResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [messageVisible, setMessageVisible] = useState<boolean>(false);

    const [suspendingUserId, setSuspendingUserId] = useState<number | null>(null);
    const [suspendingDuration, setSuspendingDuration] = useState<string>("");  
    const [suspending, setSuspending] = useState<boolean>(false);


  

  function suspend(id: number,  duration: string) {
    changeUserStatus.mutate({ id, status:"SUSPENDED", duration: Number(duration)});
  }
  function ban(id: number,  duration: number) {
    changeUserStatus.mutate({ id, status:"BANNED", duration});
  }
  function activate(id: number,  duration: number) {
    changeUserStatus.mutate({ id, status:"ACTIVE", duration});
  }
  function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
function handleSuspending(userId:number) {
  setSuspending(true);

  setSuspendingUserId(userId);

}

function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
  const value = event.target.value;
  console.log(value);
  
  setSuspendingDuration(value);

}
function handleClose() {
  setSuspending(false);
  setSuspendingDuration("");
  setSuspendingUserId(null);

}
  const {
    adminUsersQuery,
    changeUserStatus,
  } = useAdminUsers(selectedTab);

  const { data: users, isLoading, isError, error } = adminUsersQuery;

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
              <th>Status</th>
              <th>Suspended until</th>
              <th>Actions</th>


            </tr>
          </thead>
          <tbody>
            {users?.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.role}</td>
                <td>{user.status ?? "-"}</td>
                <td>{user.suspendedUntil ? formatDateTime(user.suspendedUntil): "-"}</td>

                
                <td>
                  <button onClick={()=> handleSuspending(user.id)}> Suspend </button>
                  <button onClick={()=> ban(user.id, 1)}> Ban </button>
                  <button onClick={()=> activate(user.id, 1)}> Activate </button>
                </td>


              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {suspending && suspendingUserId &&(
              <UserFormModal
              duration = {suspendingDuration}
              submitError={null}
              onChange={handleChange}       
                
                isSubmitting={changeUserStatus.isPending}
                onSubmit={() => suspend(suspendingUserId, suspendingDuration)}
                onClose={handleClose}
              />
            )}

      <BackToAdminButton>

      </BackToAdminButton>

    </main>
  );
}
