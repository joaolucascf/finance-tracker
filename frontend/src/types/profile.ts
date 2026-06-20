export type Profile = {
  name: string;
  email: string;
  nickname: string | null;
  birthDate: string | null;
  monthlyIncome: number | null;
  maritalStatus: string | null;
  photoBase64: string | null;
  photoType: string | null;
};

export type ProfileFormState = {
  nickname: string;
  birthDate: string;
  monthlyIncome: string;
  maritalStatus: string;
};

export const MARITAL_STATUS_OPTIONS = [
  { value: "SINGLE", label: "Solteiro(a)" },
  { value: "MARRIED", label: "Casado(a)" },
  { value: "STABLE_UNION", label: "União Estável" },
  { value: "DIVORCED", label: "Divorciado(a)" },
  { value: "WIDOWED", label: "Viúvo(a)" },
] as const;

export function getPhotoSrc(profile: Pick<Profile, "photoBase64" | "photoType"> | null): string | null {
  if (!profile?.photoBase64 || !profile?.photoType) return null;
  return `data:${profile.photoType};base64,${profile.photoBase64}`;
}
