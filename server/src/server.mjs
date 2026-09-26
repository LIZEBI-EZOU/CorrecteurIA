import { app } from "./app.mjs";

const port = Number(process.env.PORT || 10000);
app.listen(port, "0.0.0.0", () => {
  console.log(JSON.stringify({
    event: "startup",
    service: "CorrecteurIA Server",
    port,
    model: process.env.OPENAI_MODEL || "gpt-5.6-luna"
  }));
});
