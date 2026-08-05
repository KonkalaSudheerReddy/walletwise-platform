import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const require = createRequire(import.meta.url);
const { chromium } = require("../frontend/node_modules/@playwright/test");
const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const outputDirectory = path.resolve(scriptDirectory, "../docs/images");
const baseUrl = (process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

const captures = [
  ["/dashboard", "dashboard.jpg"],
  ["/wallets", "wallets.jpg"],
  ["/transactions", "transactions.jpg"],
  ["/budgets", "budgets.jpg"],
  ["/analytics", "analytics.jpg"],
];

await mkdir(outputDirectory, { recursive: true });
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 1440, height: 960 },
  colorScheme: "light",
  reducedMotion: "reduce",
  locale: "en-US",
  timezoneId: "UTC",
});
const page = await context.newPage();

try {
  await page.goto(`${baseUrl}/login`, { waitUntil: "networkidle" });
  await page.screenshot({
    path: path.join(outputDirectory, "login.jpg"),
    type: "jpeg",
    quality: 86,
    fullPage: true,
  });

  await page.getByRole("button", { name: "Use demo account" }).click();
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL("**/dashboard");
  await page.getByRole("heading").first().waitFor();

  for (const [route, filename] of captures) {
    await page.goto(`${baseUrl}${route}`, { waitUntil: "networkidle" });
    await page.getByRole("heading").first().waitFor();
    await page.screenshot({
      path: path.join(outputDirectory, filename),
      type: "jpeg",
      quality: 86,
      fullPage: true,
    });
  }

  console.log(`Captured verified demo screenshots in ${outputDirectory}`);
} finally {
  await browser.close();
}

