export function money(value: number | string | undefined | null, currency = "SEK") {
  const numberValue = Number(value ?? 0);

  return new Intl.NumberFormat("sv-SE", {
    style: "currency",
    currency
  }).format(numberValue);
}
