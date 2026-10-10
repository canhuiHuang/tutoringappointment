import { createBrowserRouter } from "react-router";
import Layout, { RequireRole, StartPage } from "@/pages/Layout";
import LoginPage from "@/pages/LoginPage";
import CustomerPage from "@/pages/CustomerPage";
import MyAppointmentsPage from "@/pages/MyAppointmentsPage";
import ProviderPage from "@/pages/ProviderPage";
import ManageSlotsPage from "@/pages/ManageSlotsPage";

export const router = createBrowserRouter([
  {
    Component: Layout,
    children: [
      { path: "/", Component: StartPage },
      { path: "/login", Component: LoginPage },
      {
        element: <RequireRole role="STUDENT" />,
        children: [
          { path: "/customer", Component: CustomerPage },
          { path: "/customer/appointments", Component: MyAppointmentsPage },
        ],
      },
      {
        element: <RequireRole role="TUTOR" />,
        children: [
          { path: "/provider", Component: ProviderPage },
          { path: "/provider/slots", Component: ManageSlotsPage },
        ],
      },
      { path: "*", Component: StartPage },
    ],
  },
]);
