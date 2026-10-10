import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { Session } from "@/utils/interfaces";

// Restore authentication from the server session, not localStorage.
const initialState: Session = { user: null, providerId: null };
const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setSession: (_state, action: PayloadAction<Session>) => action.payload,
  },
});
export const { setSession } = authSlice.actions;
export default authSlice.reducer;
