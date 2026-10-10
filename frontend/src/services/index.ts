import axios from "axios";
import { store } from "@/store/store";
import { setSession } from "@/store/Slices/authSlice";

const api = axios.create({ baseURL: "/", withCredentials: true });
api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 401) {
        store.dispatch(setSession({ user: null, providerId: null }));
      }
      return Promise.reject(
        new Error(error.response?.data?.message ?? "Cannot reach the server."),
      );
    }
    return Promise.reject(error);
  },
);
export default api;
