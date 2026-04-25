export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}

export const metadata = {
  title: "Finance Tracker",
  description: "Controle suas finanças",
};