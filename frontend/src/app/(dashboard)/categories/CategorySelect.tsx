import { Category } from "@/types/categories";

type Props = {
  value: number | null;
  onChange: (value: number | null) => void;
  categories: Category[];
  placeholder?: string;
  disabled?: boolean;
};

export function CategorySelect({
  value,
  onChange,
  categories,
  placeholder,
  disabled,
}: Props) {
  function handleChange(e: React.ChangeEvent<HTMLSelectElement>) {
    const rawValue = e.target.value;
    if (rawValue === "") {
      onChange(null);
      return;
    }
    onChange(Number(rawValue));
  }

  return (
    <select
      value={value ?? ""}
      onChange={handleChange}
      disabled={disabled}
      className="w-full bg-[var(--color-raised)] border border-[var(--color-border)] rounded-lg px-3 py-2.5 text-sm text-[var(--color-text)] focus:outline-none focus:border-[var(--color-teal)] transition-colors disabled:opacity-50 cursor-pointer"
    >
      <option value="">{placeholder ?? "Sem categoria"}</option>
      {categories.map((category) => (
        <option key={category.id} value={category.id}>
          {category.name}
        </option>
      ))}
    </select>
  );
}
