import { useEffect, useState } from "react";
import { getProfile } from "@/services/profile";
import { Profile } from "@/types/profile";

export function useProfile() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getProfile()
      .then(setProfile)
      .catch(() => setProfile(null))
      .finally(() => setLoading(false));
  }, []);

  return { profile, loading, setProfile };
}
