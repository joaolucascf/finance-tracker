import {
  acceptFriendRequest,
  getFriendRequests,
  getFriends,
  rejectFriendRequest,
} from "@/services/friends";
import { ApiError } from "@/types/api-error";
import { Friend, FriendRequestItem } from "@/types/friends";
import { useEffect, useState } from "react";

export function useFriends() {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [requests, setRequests] = useState<FriendRequestItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);

    try {
      const [friendsData, requestsData] = await Promise.all([
        getFriends(),
        getFriendRequests(),
      ]);
      setFriends(friendsData ?? []);
      setRequests(requestsData ?? []);
    } catch (err) {
      setError((err as ApiError)?.message ?? "Erro ao carregar amigos");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function accept(id: number) {
    const friend: Friend = await acceptFriendRequest(id);
    setRequests((prev) => prev.filter((r) => r.id !== id));
    setFriends((prev) => [...prev, friend]);
  }

  async function reject(id: number) {
    await rejectFriendRequest(id);
    setRequests((prev) => prev.filter((r) => r.id !== id));
  }

  return {
    friends,
    requests,
    loading,
    error,
    accept,
    reject,
    reload: load,
  };
}
