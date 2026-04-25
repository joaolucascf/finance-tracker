type AuthLayoutProps = {
  title: string;
  description?: string;
  children: React.ReactNode;
};

export default function AuthLayout({
  title,
  description,
  children,
}: AuthLayoutProps) {
  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "1.5rem",
          width: "100%",
          maxWidth: "320px",
        }}
      >
        <div style={{ textAlign: "center" }}>
          <h2 style={{ marginBottom: "0.5rem" }}>{title}</h2>
          {description && <p>{description}</p>}
        </div>

        {children}
      </div>
    </div>
  );
}