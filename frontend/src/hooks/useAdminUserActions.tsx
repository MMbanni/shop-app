import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api } from "../lib/api";
import { AdminUserTab, UpdateUserStatusRequest} from "../types";


export function useAdminUsers(selectedTab: AdminUserTab){
  const queryClient = useQueryClient();

  function refreshUserList() {
    return Promise.all([
      queryClient.invalidateQueries({ queryKey: ["admin-users"] })
    ]);
  }

  const adminUsersQuery=useQuery({
    queryKey: ["admin-users", selectedTab],
    queryFn: () => api.adminGetUsers(selectedTab),
  });

  
    const changeUserStatus = useMutation({
      mutationFn: (request: UpdateUserStatusRequest) => api.changeUserStatus(request),  
      onSuccess: refreshUserList
    });


  return {
    adminUsersQuery,
    changeUserStatus,
  }

}
  
