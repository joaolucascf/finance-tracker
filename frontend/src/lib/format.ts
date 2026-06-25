export function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
}

export function formatDay(dateStr: string) {
  return `Dia ${new Date(dateStr + "T00:00:00").getDate()}`;
}
