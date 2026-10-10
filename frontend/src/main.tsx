import { createRoot } from "react-dom/client";
import { RouterProvider } from "react-router";
import { router } from "./routes";

import "@/styles/index.css";
import { Provider } from "react-redux";
import { store } from "./store/store";

createRoot(document.getElementById("root")!).render(
  <Provider store={store}>
    <RouterProvider router={router} />
  </Provider>,
);
